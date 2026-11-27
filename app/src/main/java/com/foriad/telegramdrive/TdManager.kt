package com.foriad.telegramdrive

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File

/**
 * Pro mode engine: logs in as the user's own Telegram ACCOUNT via TDLib
 * (the same library the official Telegram apps use). This is what allows
 * single-file uploads up to Telegram's account limits - 2 GB for free
 * accounts, 4 GB for Telegram Premium - instead of the 50 MB bot limit.
 *
 * Files are sent to the user's Saved Messages (a chat only they can see).
 * The session is stored by TDLib in the app's private folder; the user
 * stays logged in across restarts until they log out.
 */
object TdManager {

    enum class AuthStage { NOT_STARTED, WAIT_PHONE, WAIT_CODE, WAIT_PASSWORD, READY, ERROR }

    interface Listener {
        fun onAuthStage(stage: AuthStage, message: String)
        fun onAccount(name: String, isPremium: Boolean, userId: Long)
        fun onUploadProgress(fileKey: Int, percent: Int)
        fun onUploadFinished(fileKey: Int, tdFileId: Int, tdMessageId: Long)
        fun onUploadFailed(fileKey: Int, reason: String)
        fun onDownloadProgress(fileKey: Int, percent: Int)
        fun onDownloadFinished(fileKey: Int, localPath: String)
        fun onDownloadFailed(fileKey: Int, reason: String)
    }

    private val main = Handler(Looper.getMainLooper())
    private var client: Client? = null
    private var listener: Listener? = null
    private var appContext: Context? = null
    private var apiId = 0
    private var apiHash = ""
    var myUserId: Long = 0
        private set

    // Pending transfers, keyed by TDLib file id.
    private data class Pending(val isUpload: Boolean)
    private val pending = mutableMapOf<Int, Pending>()
    private val uploadMessageIds = mutableMapOf<Int, Long>() // fileId -> messageId (from send result)

    fun setListener(l: Listener?) { listener = l }

    fun isStarted() = client != null

    fun start(context: Context, apiId: Int, apiHash: String) {
        if (client != null) return
        this.apiId = apiId
        this.apiHash = apiHash
        appContext = context.applicationContext
        try {
            client = Client.create({ obj -> onUpdate(obj) }, null, null)
        } catch (e: Throwable) {
            post { listener?.onAuthStage(AuthStage.ERROR, "Could not start Telegram engine: ${e.message}") }
        }
    }

    fun stop() {
        try { client?.send(TdApi.Close(), null, null) } catch (_: Throwable) { }
        client = null
        pending.clear()
        uploadMessageIds.clear()
    }

    private fun onUpdate(obj: TdApi.Object) {
        when (obj) {
            is TdApi.UpdateAuthorizationState -> handleAuth(obj.authorizationState)
            is TdApi.UpdateFile -> handleFileUpdate(obj.file)
            is TdApi.UpdateMessageSendSucceeded -> {
                val content = obj.message.content
                if (content is TdApi.MessageDocument) {
                    val fid = content.document.document.id
                    if (pending[fid]?.isUpload == true) {
                        uploadMessageIds[fid] = obj.message.id
                        // Upload may already be complete or still sending;
                        // completion is confirmed by the file update too.
                        checkUploadComplete(fid, content.document.document)
                    }
                }
            }
            is TdApi.UpdateMessageSendFailed -> {
                val content = obj.oldMessage.content
                if (content is TdApi.MessageDocument) {
                    val fid = content.document.document.id
                    if (pending.remove(fid)?.isUpload == true) {
                        post { listener?.onUploadFailed(fid, obj.error?.message ?: "Send failed") }
                    }
                }
            }
        }
    }

    private fun handleAuth(state: TdApi.AuthorizationState) {
        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                val ctx = appContext ?: return
                val dbDir = File(ctx.filesDir, "tdlib").apply { mkdirs() }.absolutePath
                val filesDir = File(ctx.filesDir, "tdlib-files").apply { mkdirs() }.absolutePath
                send(TdApi.SetTdlibParameters(
                    false, dbDir, filesDir, byteArrayOf(),
                    true, true, true, false,
                    apiId, apiHash, "en",
                    Build.MODEL ?: "Android", Build.VERSION.RELEASE ?: "14", "1.1.0"
                )) { obj ->
                    if (obj is TdApi.Error) post {
                        listener?.onAuthStage(AuthStage.ERROR, "Telegram rejected the API ID / API Hash (${obj.message}). Check them at my.telegram.org and try again.")
                    }
                }
            }
            is TdApi.AuthorizationStateWaitPhoneNumber ->
                post { listener?.onAuthStage(AuthStage.WAIT_PHONE, "Enter your Telegram phone number (with country code).") }
            is TdApi.AuthorizationStateWaitCode ->
                post { listener?.onAuthStage(AuthStage.WAIT_CODE, "Telegram sent you a login code (in the Telegram app or SMS). Enter it here.") }
            is TdApi.AuthorizationStateWaitPassword ->
                post { listener?.onAuthStage(AuthStage.WAIT_PASSWORD, "Your account has 2-Step Verification. Enter your Telegram password.") }
            is TdApi.AuthorizationStateReady -> {
                post { listener?.onAuthStage(AuthStage.READY, "Logged in ✅") }
                send(TdApi.GetMe()) { obj ->
                    if (obj is TdApi.User) {
                        myUserId = obj.id
                        val name = (obj.firstName + " " + obj.lastName).trim()
                        post { listener?.onAccount(name, obj.isPremium, obj.id) }
                    }
                }
            }
            is TdApi.AuthorizationStateClosed ->
                post { listener?.onAuthStage(AuthStage.NOT_STARTED, "Logged out.") }
            else ->
                post { listener?.onAuthStage(AuthStage.NOT_STARTED, "State: ${state.javaClass.simpleName}") }
        }
    }

    fun sendPhone(phone: String) {
        send(TdApi.SetAuthenticationPhoneNumber(phone,
            TdApi.PhoneNumberAuthenticationSettings(false, false, false, false, false, null, arrayOf())
        )) { obj -> if (obj is TdApi.Error) post { listener?.onAuthStage(AuthStage.WAIT_PHONE, "Phone number problem: ${obj.message}") } }
    }

    fun sendCode(code: String) {
        send(TdApi.CheckAuthenticationCode(code)) { obj ->
            if (obj is TdApi.Error) post { listener?.onAuthStage(AuthStage.WAIT_CODE, "That code didn't work: ${obj.message}") }
        }
    }

    fun sendPassword(password: String) {
        send(TdApi.CheckAuthenticationPassword(password)) { obj ->
            if (obj is TdApi.Error) post { listener?.onAuthStage(AuthStage.WAIT_PASSWORD, "That password didn't work: ${obj.message}") }
        }
    }

    fun logout() {
        send(TdApi.LogOut()) { }
        myUserId = 0
    }

    /** Upload one file (any size up to the account limit) to Saved Messages. */
    fun upload(localPath: String, displayName: String) {
        if (myUserId == 0L) {
            post { listener?.onUploadFailed(-1, "Not logged in yet") }
            return
        }
        val content = TdApi.InputMessageDocument(
            TdApi.InputDocument(TdApi.InputFileLocal(localPath), null, false),
            TdApi.FormattedText("📁 Telegram Drive Pro", arrayOf())
        )
        send(TdApi.SendMessage(myUserId, null, null, null, null, content)) { obj ->
            when (obj) {
                is TdApi.Message -> {
                    val doc = (obj.content as? TdApi.MessageDocument)?.document?.document
                    if (doc != null) {
                        pending[doc.id] = Pending(true)
                        uploadMessageIds[doc.id] = obj.id
                        checkUploadComplete(doc.id, doc)
                    } else {
                        post { listener?.onUploadFailed(-1, "Telegram did not accept the file") }
                    }
                }
                is TdApi.Error -> post { listener?.onUploadFailed(-1, obj.message) }
            }
        }
    }

    fun download(tdFileId: Int) {
        pending[tdFileId] = Pending(false)
        send(TdApi.DownloadFile(tdFileId, 32, 0, 0, false)) { obj ->
            if (obj is TdApi.Error) {
                pending.remove(tdFileId)
                post { listener?.onDownloadFailed(tdFileId, obj.message) }
            } else if (obj is TdApi.File) {
                handleFileUpdate(obj)
            }
        }
    }

    fun deleteMessage(tdMessageId: Long) {
        if (myUserId == 0L) return
        send(TdApi.DeleteMessages(myUserId, longArrayOf(tdMessageId), true)) { }
    }

    private fun handleFileUpdate(file: TdApi.File) {
        val p = pending[file.id] ?: return
        if (p.isUpload) {
            val total = if (file.size > 0) file.size else file.expectedSize
            val sent = file.remote?.uploadedSize ?: 0
            if (total > 0) post { listener?.onUploadProgress(file.id, (sent * 100 / total).toInt().coerceIn(0, 100)) }
            checkUploadComplete(file.id, file)
        } else {
            val total = if (file.size > 0) file.size else file.expectedSize
            val got = file.local?.downloadedSize ?: 0
            if (total > 0) post { listener?.onDownloadProgress(file.id, (got * 100 / total).toInt().coerceIn(0, 100)) }
            if (file.local?.isDownloadingCompleted == true) {
                pending.remove(file.id)
                val path = file.local?.path ?: ""
                post { listener?.onDownloadFinished(file.id, path) }
            }
        }
    }

    private fun checkUploadComplete(fileId: Int, file: TdApi.File) {
        if (file.remote?.isUploadingCompleted == true) {
            pending.remove(fileId)
            val mid = uploadMessageIds.remove(fileId) ?: 0L
            post { listener?.onUploadFinished(fileId, fileId, mid) }
        }
    }

    private fun send(query: TdApi.Function<*>, handler: (TdApi.Object) -> Unit) {
        val c = client
        if (c == null) {
            post { listener?.onAuthStage(AuthStage.ERROR, "Telegram engine is not running. Go back and open Pro mode again.") }
            return
        }
        try {
            c.send(query, { obj -> handler(obj) }, null)
        } catch (e: Throwable) {
            post { listener?.onAuthStage(AuthStage.ERROR, "Telegram engine error: ${e.message}") }
        }
    }

    private fun post(block: () -> Unit) = main.post(block)
}
