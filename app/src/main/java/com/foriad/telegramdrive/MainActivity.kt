package com.foriad.telegramdrive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.foriad.telegramdrive.databinding.ActivityMainBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs
    private val handler = Handler(Looper.getMainLooper())

    private var setupStep = 1
    private var pendingToken = ""
    private var pendingBotName = ""
    private var pendingChatId = ""
    private var detectAttempts = 0

    private var files: MutableList<DriveFile> = mutableListOf()
    private var folders: MutableList<String> = mutableListOf()
    private var currentFolder = "Home"
    private var isGrid = false
    private lateinit var adapter: FileAdapter

    private var interstitialAd: InterstitialAd? = null
    private var lastInterstitialShownAt = 0L

    // ---- Premium ($10 lifetime via Google Play Billing) ----
    private lateinit var billing: BillingManager
    private var premiumPriceText: String? = null // real localized price from Play
    private var premiumDialogStatus: android.widget.TextView? = null
    private var billingAvailable: Boolean? = null // null = not checked yet

    private val billingListener = object : BillingManager.Listener {
        override fun onPriceLoaded(formattedPrice: String) {
            premiumPriceText = formattedPrice
            billingAvailable = true
            updatePremiumUi()
            premiumDialogStatus?.text = "Price from Google Play: $formattedPrice — one time, yours forever."
        }

        override fun onPremiumOwned() {
            val wasPremium = prefs.isPremium
            prefs.isPremium = true
            billingAvailable = true
            updatePremiumUi()
            premiumDialogStatus?.text = "✅ Premium is active — all ads are removed forever. Thank you!"
            if (!wasPremium) toast("Premium active ✅ All ads removed forever.")
        }

        override fun onBillingUnavailable(reason: String) {
            billingAvailable = false
            // Friendly sideload message — only shown inside the Premium dialog,
            // never as a surprise popup. Sideloaded APKs (GitHub Releases)
            // cannot use Google Play Billing; nothing is fake-unlocked.
            premiumDialogStatus?.text =
                "Purchases are available in the Google Play version of this app.\n\n" +
                    "This copy was installed from outside Google Play (e.g. a GitHub Release APK), " +
                    "so Google Play Billing can't run here and Premium can't be bought in this copy. " +
                    "Install the app from Google Play to buy Premium — your Telegram Drive files and setup are unaffected."
        }

        override fun onBillingError(message: String) {
            // Only surface errors while the Premium dialog is open (user-initiated).
            premiumDialogStatus?.let { it.text = message }
        }
    }

    private val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) uploadUri(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs(this)
        billing = BillingManager(this, billingListener)

        // Record any crash so the next launch can show what happened instead
        // of silently restarting. Never let the recorder itself crash the app.
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                prefs.lastCrash = "${error.javaClass.simpleName}: ${error.message ?: ""} @ " +
                    (error.stackTrace.firstOrNull()?.let { "${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}" } ?: "")
            } catch (_: Exception) { }
            previousHandler?.uncaughtException(thread, error)
        }

        // Ads must never be able to crash the app (e.g. Play services issues).
        if (!prefs.isPremium) {
            try {
                MobileAds.initialize(this) {}
                loadInterstitial()
            } catch (e: Exception) {
                // Drive works fine without ads if the SDK fails to start.
            }
        }

        setupWizardListeners()
        setupDriveListeners()

        if (prefs.isConnected) showDrive() else showSetup()
    }

    override fun onDestroy() {
        if (::billing.isInitialized) billing.end()
        super.onDestroy()
    }

    // ---------------- SETUP WIZARD ----------------

    private fun showSetup() {
        binding.setupContainer.visibility = View.VISIBLE
        binding.driveContainer.visibility = View.GONE
        goToStep(1)
        // If the previous session crashed, say so here (helps support).
        val crash = prefs.lastCrash
        if (crash.isNotBlank()) {
            binding.tokenStatus.text = "Last time the app closed with an error: $crash\nYou can continue setup normally."
            prefs.lastCrash = ""
        }
    }

    private fun goToStep(step: Int) {
        setupStep = step
        binding.step1.visibility = if (step == 1) View.VISIBLE else View.GONE
        binding.step2.visibility = if (step == 2) View.VISIBLE else View.GONE
        binding.step3.visibility = if (step == 3) View.VISIBLE else View.GONE
        binding.setupProgress.progress = step * 33
        binding.setupStepLabel.text = "Step $step of 3"
    }

    private fun setupWizardListeners() = with(binding) {
        copyNewbotButton.setOnClickListener {
            copyToClipboard("/newbot")
            toast("Copied: /newbot — paste it in BotFather chat")
        }
        openBotFatherButton.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/BotFather")))
        }
        checkTokenButton.setOnClickListener {
            val token = tokenInput.text.toString().trim()
            if (token.length < 20 || !token.contains(":")) {
                tokenStatus.text = "That doesn't look like a full token. Copy the whole token BotFather sent you."
                return@setOnClickListener
            }
            tokenStatus.text = "Checking your token…"
            TelegramApi(token).getMe(
                onOk = { me -> runOnUiThread {
                    pendingToken = token
                    pendingBotName = "@" + me.optString("username")
                    tokenStatus.text = "✅ Token works! Your bot is $pendingBotName. Tap Next."
                    nextToStep2Button.isEnabled = true
                } },
                onError = { err -> runOnUiThread {
                    tokenStatus.text = "That token didn't work ($err). Copy the whole token from @BotFather and try again."
                } }
            )
        }
        nextToStep2Button.setOnClickListener { goToStep(2) }

        manualChatToggle.setOnClickListener {
            manualChatInput.visibility =
                if (manualChatInput.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        detectChatButton.setOnClickListener {
            detectAttempts = 0
            detectStatus.text = "Looking for your group… send any message in the group now (like: hi)."
            pollForChatId()
        }
        useManualChatButton.setOnClickListener {
            val id = manualChatInput.text.toString().trim()
            if (id.isBlank()) { toast("Paste the Chat ID first"); return@setOnClickListener }
            pendingChatId = id
            showSummaryAndGoToStep3()
        }
        connectButton.setOnClickListener { connect() }
        disconnectButton.setOnClickListener {
            prefs.disconnect()
            toast("Disconnected. Your files are still in your Telegram group.")
            showSetup()
        }
    }

    private fun pollForChatId() {
        detectAttempts++
        TelegramApi(pendingToken).getUpdates(
            onOk = { updates -> runOnUiThread {
                val found = TelegramApi.findGroupChatId(updates)
                if (found != null) {
                    pendingChatId = found
                    binding.detectStatus.text = "✅ Found your group! Chat ID: $found"
                    showSummaryAndGoToStep3()
                } else if (detectAttempts < 10) {
                    binding.detectStatus.text = "Not found yet — send a message in the group, looking again… ($detectAttempts)"
                    handler.postDelayed({ pollForChatId() }, 3000)
                } else {
                    binding.detectStatus.text = "Still not found. Make sure the bot is an ADMIN in the group, send a new message, or use manual Chat ID below."
                }
            } },
            onError = { err -> runOnUiThread { binding.detectStatus.text = "Error: $err" } }
        )
    }

    private fun showSummaryAndGoToStep3() {
        binding.summaryText.text = "Bot: $pendingBotName\nGroup Chat ID: $pendingChatId\n\nTap Connect — we will post a small confirmation message in your group."
        goToStep(3)
    }

    private fun connect() {
        TelegramApi(pendingToken).sendMessage(pendingChatId,
            "✅ Telegram Drive connected! Files you upload in the app will appear here.",
            onOk = { runOnUiThread {
                prefs.saveConnection(pendingToken, pendingChatId, pendingBotName)
                toast("Connected!")
                showDrive()
            } },
            onError = { err -> runOnUiThread {
                binding.summaryText.text = "Connect failed: $err\n\nCheck: is the bot an admin in the group? Is the Chat ID correct?"
            } }
        )
    }

    // ---------------- DRIVE ----------------

    private fun showDrive() {
        binding.setupContainer.visibility = View.GONE
        binding.driveContainer.visibility = View.VISIBLE
        files = prefs.loadFiles()
        folders = prefs.loadFolders()
        binding.driveTitle.text = "Telegram Drive"
        binding.driveSubtitle.text = "Connected as ${prefs.botName} • files live in your private Telegram group"

        adapter = FileAdapter(emptyList(), onClick = { f -> openOrDownload(f) }, onLongClick = { f -> fileActions(f) })
        binding.fileList.layoutManager = LinearLayoutManager(this)
        binding.fileList.adapter = adapter
        refreshFolderSpinner()
        refreshList()
        updatePremiumUi()
        // Ads and billing are extras - if either fails on this device, the
        // Drive itself must still open. Guard both so they can never take
        // the Drive screen down with them.
        try { loadBannerAd() } catch (e: Exception) { binding.adView.visibility = View.GONE }
        // Quietly connect to Play: loads the real Premium price and
        // auto-restores Premium if this Google account already bought it.
        // On sideloaded installs this fails silently (see BillingManager).
        try { billing.start() } catch (e: Exception) { /* Premium just stays unavailable */ }
    }

    private fun setupDriveListeners() = with(binding) {
        premiumButton.setOnClickListener { showPremiumDialog() }
        uploadButton.setOnClickListener { pickFile.launch("*/*") }
        noteButton.setOnClickListener { showNoteDialog() }
        newFolderButton.setOnClickListener { showNewFolderDialog() }
        gridToggleButton.setOnClickListener {
            isGrid = !isGrid
            binding.fileList.layoutManager =
                if (isGrid) GridLayoutManager(this@MainActivity, 2) else LinearLayoutManager(this@MainActivity)
        }
        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) = refreshList()
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
    }

    private fun refreshFolderSpinner() {
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, folders)
        binding.folderSpinner.adapter = adapterSpinner
        binding.folderSpinner.setSelection(folders.indexOf(currentFolder).coerceAtLeast(0))
        binding.folderSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                currentFolder = folders[pos]; refreshList()
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }
    }

    private fun refreshList() {
        if (!::adapter.isInitialized) return
        val q = binding.searchInput.text?.toString()?.trim().orEmpty()
        val visible = files.filter {
            (it.folder == currentFolder || currentFolder == "Home" && it.folder == "Home" || currentFolder == "All")
                .let { inFolder -> inFolder || currentFolder == "Home" }
        }.filter { q.isBlank() || it.name.contains(q, ignoreCase = true) || it.noteText.contains(q, ignoreCase = true) }
            .sortedByDescending { it.uploadedAt }
        // Folder filter: "Home" shows only Home files; other folders show only their files.
        val byFolder = if (currentFolder == "Home") visible.filter { it.folder == "Home" } else visible.filter { it.folder == currentFolder }
        adapter.submit(byFolder)
        binding.emptyText.visibility = if (byFolder.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun uploadUri(uri: Uri) {
        val name = queryName(uri) ?: "file-${System.currentTimeMillis()}"
        val size = querySize(uri)
        if (size > 50L * 1024 * 1024) { toast("Telegram bots can upload up to 50 MB per file."); return }
        toast("Uploading $name…")
        Thread {
            try {
                val tmp = File(cacheDir, "upload_${System.currentTimeMillis()}")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tmp).use { out -> input.copyTo(out) }
                } ?: throw Exception("Could not read file")
                val api = TelegramApi(prefs.botToken)
                val caption = "📁 Telegram Drive • $currentFolder"
                val done = { msg: org.json.JSONObject ->
                    val (fileId, kind) = TelegramApi.extractFileId(msg)
                    val driveFile = DriveFile(
                        id = UUID.randomUUID().toString(), name = name,
                        kind = if (kind == "photo") "photo" else kindFromName(name),
                        sizeBytes = tmp.length(), folder = currentFolder,
                        telegramFileId = fileId, telegramMessageId = msg.optInt("message_id"),
                        uploadedAt = System.currentTimeMillis()
                    )
                    runOnUiThread {
                        files.add(0, driveFile); prefs.saveFiles(files); refreshList()
                        tmp.delete()
                        toast("Uploaded ✅")
                        maybeShowInterstitial()
                    }
                }
                val fail = { err: String -> runOnUiThread { toast("Upload failed: $err"); tmp.delete() } }
                val mime = contentResolver.getType(uri) ?: ""
                if (mime.startsWith("image/")) api.sendPhoto(prefs.chatId, tmp, name, caption, done, fail)
                else api.sendDocument(prefs.chatId, tmp, name, caption, done, fail)
            } catch (e: Exception) {
                runOnUiThread { toast("Upload failed: ${e.message}") }
            }
        }.start()
    }

    private fun kindFromName(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg", "png", "gif", "webp" -> "photo"
            "mp4", "mkv", "mov", "webm" -> "video"
            "mp3", "wav", "ogg", "m4a" -> "audio"
            else -> "document"
        }
    }

    private fun openOrDownload(file: DriveFile) {
        if (file.kind == "note" || file.kind == "link") {
            AlertDialog.Builder(this).setTitle(file.name).setMessage(file.noteText)
                .setPositiveButton("OK", null).show()
            return
        }
        if (file.telegramFileId.isBlank()) { toast("No Telegram file ID saved for this item"); return }
        toast("Getting download link…")
        TelegramApi(prefs.botToken).getFile(file.telegramFileId,
            onOk = { _, downloadUrl -> runOnUiThread {
                // Opens Telegram's own file URL in the browser/downloader.
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)))
            } },
            onError = { err -> runOnUiThread { toast("Download failed: $err") } }
        )
    }

    private fun fileActions(file: DriveFile) {
        val actions = arrayOf("Download / Open", "Delete")
        AlertDialog.Builder(this).setTitle(file.name).setItems(actions) { _, which ->
            when (which) {
                0 -> openOrDownload(file)
                1 -> {
                    files.remove(file); prefs.saveFiles(files); refreshList()
                    if (file.telegramMessageId != 0) {
                        TelegramApi(prefs.botToken).deleteMessage(prefs.chatId, file.telegramMessageId, {}, {})
                    }
                    toast("Deleted")
                }
            }
        }.show()
    }

    private fun showNoteDialog() {
        val input = android.widget.EditText(this).apply { hint = "Write a note or paste a link…" }
        AlertDialog.Builder(this).setTitle("New note").setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save to Drive") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isBlank()) return@setPositiveButton
                TelegramApi(prefs.botToken).sendMessage(prefs.chatId, "📝 $text",
                    onOk = { msg -> runOnUiThread {
                        val isLink = text.startsWith("http://") || text.startsWith("https://")
                        files.add(0, DriveFile(
                            id = UUID.randomUUID().toString(),
                            name = if (isLink) text.take(60) else text.lineSequence().firstOrNull()?.take(60) ?: "Note",
                            kind = if (isLink) "link" else "note", sizeBytes = 0, folder = currentFolder,
                            telegramFileId = "", telegramMessageId = msg.optInt("message_id"),
                            uploadedAt = System.currentTimeMillis(), noteText = text))
                        prefs.saveFiles(files); refreshList(); toast("Note saved ✅")
                    } },
                    onError = { err -> runOnUiThread { toast("Failed: $err") } }
                )
            }.show()
    }

    private fun showNewFolderDialog() {
        val input = android.widget.EditText(this).apply { hint = "Folder name" }
        AlertDialog.Builder(this).setTitle("New folder").setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isBlank()) return@setPositiveButton
                if (name !in folders) { folders.add(name); prefs.saveFolders(folders) }
                currentFolder = name; refreshFolderSpinner(); refreshList()
            }.show()
    }

    private fun queryName(uri: Uri): String? = queryColumn(uri, OpenableColumns.DISPLAY_NAME)
    private fun querySize(uri: Uri): Long = queryColumn(uri, OpenableColumns.SIZE)?.toLongOrNull() ?: 0L
    private fun queryColumn(uri: Uri, column: String): String? {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(column)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        } finally { cursor?.close() }
    }

    // ---------------- PREMIUM ($10 LIFETIME) ----------------

    /** Reflects Premium state: premium users never see or load any ad. */
    private fun updatePremiumUi() {
        if (!::binding.isInitialized) return
        if (prefs.isPremium) {
            binding.adView.visibility = View.GONE
            binding.premiumButton.text = "⭐ Premium active — ads removed forever ✅"
        } else {
            binding.adView.visibility = View.VISIBLE
            val price = premiumPriceText ?: PremiumConfig.FALLBACK_PRICE_TEXT
            binding.premiumButton.text = "⭐ Go Premium — $price forever (no ads)"
        }
    }

    private fun showPremiumDialog() {
        val price = premiumPriceText ?: PremiumConfig.FALLBACK_PRICE_TEXT
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
        }
        val info = android.widget.TextView(this).apply {
            text = if (prefs.isPremium) {
                "You are Premium ⭐\n\nAll ads are removed forever on this account. " +
                    "Reinstalling or changing phones? Tap Restore Purchase and Premium comes back free."
            } else {
                "FREE\n• Full Telegram Drive — uploads, folders, notes, search\n• Ads (a banner + an occasional full-screen ad)\n\n" +
                    "PREMIUM — $price, ONE time, forever\n• No banner ads. No full-screen ads. Ever.\n" +
                    "• Not a subscription — pay once, yours for life\n" +
                    "• Works again free on a new phone with Restore Purchase"
            }
        }
        val status = android.widget.TextView(this).apply {
            setPadding(0, 24, 0, 0)
            text = when {
                prefs.isPremium -> "✅ Premium is active on this device."
                billingAvailable == false ->
                    "Purchases are available in the Google Play version of this app. " +
                        "This copy was installed from outside Google Play, so billing can't run here."
                premiumPriceText != null -> "Price from Google Play: $premiumPriceText — one time, yours forever."
                else -> "Checking Google Play for the price…"
            }
        }
        container.addView(info)
        container.addView(status)
        premiumDialogStatus = status

        val dialog = AlertDialog.Builder(this)
            .setTitle("Telegram Drive Premium")
            .setView(container)
            .setNegativeButton("Close") { d, _ -> premiumDialogStatus = null; d.dismiss() }
        if (!prefs.isPremium) {
            dialog.setPositiveButton("Buy — $price forever") { _, _ ->
                premiumDialogStatus = null
                billing.buy(this)
            }
        }
        dialog.setNeutralButton("Restore Purchase") { _, _ ->
            premiumDialogStatus = null
            toast("Checking Google Play for your earlier purchase…")
            billing.restorePurchases()
        }
        dialog.setOnDismissListener { premiumDialogStatus = null }
        dialog.show()
        // Make sure price/product state is fresh while the dialog is open.
        billing.start()
    }

    // ---------------- ADS ----------------

    private fun loadBannerAd() {
        // PREMIUM: never load ads, keep the banner container hidden forever.
        if (prefs.isPremium) {
            binding.adView.visibility = View.GONE
            return
        }
        // Banner at the bottom of the Drive screen only (never on setup screens).
        binding.adView.visibility = View.VISIBLE
        binding.adView.adUnitId = AdsConfig.BANNER_ID
        binding.adView.loadAd(AdRequest.Builder().build())
    }

    private fun loadInterstitial() {
        if (prefs.isPremium) { interstitialAd = null; return } // PREMIUM: no interstitials, ever
        InterstitialAd.load(this, AdsConfig.INTERSTITIAL_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { interstitialAd = null /* fail silently offline */ }
            })
    }

    /** After a successful upload, at most once every 3 minutes. Never for Premium. */
    private fun maybeShowInterstitial() {
        if (prefs.isPremium) return // PREMIUM: ads removed permanently
        val now = System.currentTimeMillis()
        if (now - lastInterstitialShownAt < AdsConfig.INTERSTITIAL_MIN_INTERVAL_MS) return
        val ad = interstitialAd ?: return
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { loadInterstitial() }
            override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) { loadInterstitial() }
        }
        lastInterstitialShownAt = now
        ad.show(this)
        interstitialAd = null
    }

    // ---------------- HELPERS ----------------

    private fun copyToClipboard(text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("telegram-drive", text))
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
