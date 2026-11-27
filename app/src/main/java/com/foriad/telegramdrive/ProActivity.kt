package com.foriad.telegramdrive

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.foriad.telegramdrive.databinding.ActivityProBinding
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/** One file stored through Pro Mode (in the user's Saved Messages). */
data class ProFile(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val tdFileId: Int,
    val tdMessageId: Long,
    val uploadedAt: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("sizeBytes", sizeBytes)
        put("tdFileId", tdFileId); put("tdMessageId", tdMessageId); put("uploadedAt", uploadedAt)
    }
    companion object {
        fun fromJson(j: JSONObject) = ProFile(
            j.optString("id"), j.optString("name"), j.optLong("sizeBytes"),
            j.optInt("tdFileId"), j.optLong("tdMessageId"), j.optLong("uploadedAt")
        )
    }
}

/**
 * Pro Mode screen: Telegram ACCOUNT login (TDLib) for single-file uploads
 * up to 2 GB (free) / 4 GB (Telegram Premium). Files live in Saved Messages.
 */
class ProActivity : AppCompatActivity(), TdManager.Listener {

    private lateinit var binding: ActivityProBinding
    private val sp by lazy { getSharedPreferences("telegram_drive_pro", MODE_PRIVATE) }
    private var proFiles: MutableList<ProFile> = mutableListOf()
    private lateinit var listAdapter: ArrayAdapter<String>
    private var pendingUploadName = ""
    private var pendingUploadSize = 0L
    private var pendingUploadKey = -1
    private val downloadNames = mutableMapOf<Int, String>()

    private val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) startProUpload(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProBinding.inflate(layoutInflater)
        setContentView(binding.root)
        proFiles = loadProFiles()
        listAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        binding.proFileList.adapter = listAdapter
        refreshProList()
        wireButtons()
        TdManager.setListener(this)

        // Prefill saved API keys; if present, start the engine right away
        // (TDLib restores the previous login session automatically).
        val savedId = sp.getString("api_id", "") ?: ""
        val savedHash = sp.getString("api_hash", "") ?: ""
        if (savedId.isNotBlank()) binding.apiIdInput.setText(savedId)
        if (savedHash.isNotBlank()) binding.apiHashInput.setText(savedHash)
        if (savedId.isNotBlank() && savedHash.isNotBlank()) {
            binding.loginSection.visibility = View.VISIBLE
            binding.loginStatus.text = "Connecting to Telegram…"
            TdManager.start(this, savedId.toIntOrNull() ?: 0, savedHash)
        }
        binding.proFileList.setOnItemClickListener { _, _, pos, _ ->
            val f = proFiles.getOrNull(pos) ?: return@setOnItemClickListener
            downloadNames[f.tdFileId] = f.name
            binding.proStatus.text = "Downloading ${f.name}…"
            TdManager.download(f.tdFileId)
        }
        binding.proFileList.setOnItemLongClickListener { _, _, pos, _ ->
            val f = proFiles.getOrNull(pos) ?: return@setOnItemLongClickListener true
            AlertDialog.Builder(this).setTitle(f.name)
                .setItems(arrayOf("Download", "Delete")) { _, which ->
                    when (which) {
                        0 -> {
                            downloadNames[f.tdFileId] = f.name
                            binding.proStatus.text = "Downloading ${f.name}…"
                            TdManager.download(f.tdFileId)
                        }
                        1 -> {
                            TdManager.deleteMessage(f.tdMessageId)
                            proFiles.remove(f); saveProFiles(); refreshProList()
                            toast("Deleted")
                        }
                    }
                }.show()
            true
        }
    }

    override fun onDestroy() {
        TdManager.setListener(null)
        super.onDestroy()
    }

    private fun wireButtons() = with(binding) {
        saveApiButton.setOnClickListener {
            val id = apiIdInput.text.toString().trim()
            val hash = apiHashInput.text.toString().trim()
            if (id.toIntOrNull() == null || hash.length < 10) {
                toast("Paste your api_id (numbers) and api_hash from my.telegram.org")
                return@setOnClickListener
            }
            sp.edit().putString("api_id", id).putString("api_hash", hash).apply()
            loginSection.visibility = View.VISIBLE
            loginStatus.text = "Connecting to Telegram…"
            if (TdManager.isStarted()) TdManager.stop()
            TdManager.start(this@ProActivity, id.toInt(), hash)
        }
        sendPhoneButton.setOnClickListener {
            val phone = phoneInput.text.toString().trim()
            if (phone.length < 8) { toast("Enter your full phone number with country code"); return@setOnClickListener }
            sp.edit().putString("phone", phone).apply()
            TdManager.sendPhone(phone)
        }
        sendCodeButton.setOnClickListener {
            val code = codeInput.text.toString().trim()
            if (code.isBlank()) { toast("Enter the code Telegram sent you"); return@setOnClickListener }
            TdManager.sendCode(code)
        }
        sendPasswordButton.setOnClickListener {
            val pw = passwordInput.text.toString()
            if (pw.isBlank()) { toast("Enter your Telegram 2-Step password"); return@setOnClickListener }
            TdManager.sendPassword(pw)
        }
        uploadProButton.setOnClickListener { pickFile.launch("*/*") }
        logoutProButton.setOnClickListener {
            TdManager.logout()
            proDriveSection.visibility = View.GONE
            loginSection.visibility = View.VISIBLE
            loginStatus.text = "Logged out. Log in again whenever you want Pro Mode."
        }
    }

    // ---------- TdManager.Listener ----------

    override fun onAuthStage(stage: TdManager.AuthStage, message: String) {
        binding.loginStatus.text = message
        if (stage == TdManager.AuthStage.READY) {
            binding.loginSection.visibility = View.GONE
            binding.proDriveSection.visibility = View.VISIBLE
        }
    }

    override fun onAccount(name: String, isPremium: Boolean, userId: Long) {
        val limit = if (isPremium) "4 GB" else "2 GB"
        binding.accountText.text =
            "Logged in as $name\n${if (isPremium) "Telegram Premium ⭐" else "Free account"} — single files up to $limit.\nFiles are stored in your Saved Messages."
    }

    override fun onUploadProgress(fileKey: Int, percent: Int) {
        if (fileKey == pendingUploadKey || pendingUploadKey == -1) {
            binding.proStatus.text = "Uploading $pendingUploadName… $percent%"
        }
    }

    override fun onUploadFinished(fileKey: Int, tdFileId: Int, tdMessageId: Long) {
        val done = ProFile(
            id = java.util.UUID.randomUUID().toString(), name = pendingUploadName,
            sizeBytes = pendingUploadSize, tdFileId = tdFileId, tdMessageId = tdMessageId,
            uploadedAt = System.currentTimeMillis()
        )
        proFiles.add(0, done); saveProFiles(); refreshProList()
        binding.proStatus.text = "Uploaded ✅ $pendingUploadName"
        pendingUploadKey = -1
        toast("Uploaded ✅")
    }

    override fun onUploadFailed(fileKey: Int, reason: String) {
        binding.proStatus.text = "Upload failed: $reason"
        pendingUploadKey = -1
        toast("Upload failed: $reason")
    }

    override fun onDownloadProgress(fileKey: Int, percent: Int) {
        binding.proStatus.text = "Downloading ${downloadNames[fileKey] ?: "file"}… $percent%"
    }

    override fun onDownloadFinished(fileKey: Int, localPath: String) {
        val name = downloadNames.remove(fileKey) ?: "file"
        try {
            val src = File(localPath)
            val saved = saveToDownloads(src, name)
            binding.proStatus.text = "Saved to Downloads: $saved ✅"
            toast("Saved to Downloads ✅")
        } catch (e: Exception) {
            binding.proStatus.text = "Downloaded, but saving failed: ${e.message}"
        }
    }

    override fun onDownloadFailed(fileKey: Int, reason: String) {
        binding.proStatus.text = "Download failed: $reason"
    }

    // ---------- Upload plumbing ----------

    private fun startProUpload(uri: Uri) {
        val name = queryName(uri) ?: "file-${System.currentTimeMillis()}"
        toast("Preparing $name… (big files can take a while)")
        Thread {
            try {
                val tmp = File(cacheDir, "pro_upload_${System.currentTimeMillis()}")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tmp).use { out -> input.copyTo(out) }
                } ?: throw Exception("Could not read file")
                pendingUploadName = name
                pendingUploadSize = tmp.length()
                pendingUploadKey = -1
                runOnUiThread {
                    binding.proStatus.text = "Uploading $name… 0%"
                    TdManager.upload(tmp.absolutePath, name)
                }
            } catch (e: Exception) {
                runOnUiThread { toast("Upload failed: ${e.message}") }
            }
        }.start()
    }

    private fun queryName(uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        } finally { cursor?.close() }
    }

    private fun saveToDownloads(source: File, displayName: String): String {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw Exception("Downloads is not available")
            contentResolver.openOutputStream(uri)?.use { out ->
                source.inputStream().use { it.copyTo(out) }
            } ?: throw Exception("Could not write to Downloads")
            values.clear()
            values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
            return displayName
        }
        val dir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: filesDir
        val dest = File(dir, displayName)
        source.copyTo(dest, overwrite = true)
        return dest.name
    }

    // ---------- Local index ----------

    private fun loadProFiles(): MutableList<ProFile> {
        val raw = sp.getString("pro_files_json", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return MutableList(arr.length()) { ProFile.fromJson(arr.getJSONObject(it)) }
    }

    private fun saveProFiles() {
        val arr = JSONArray()
        proFiles.forEach { arr.put(it.toJson()) }
        sp.edit().putString("pro_files_json", arr.toString()).apply()
    }

    private fun refreshProList() {
        val labels = proFiles.map { "${it.name}\n${FileAdapter.formatSize(it.sizeBytes)} • Pro (Saved Messages)" }
        listAdapter.clear()
        listAdapter.addAll(labels)
        listAdapter.notifyDataSetChanged()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
