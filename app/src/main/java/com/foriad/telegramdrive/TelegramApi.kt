package com.foriad.telegramdrive

import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin wrapper over the Telegram Bot API: https://api.telegram.org/bot<TOKEN>/<method>
 * No server backend - the app talks directly to Telegram.
 */
class TelegramApi(private val token: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private fun url(method: String) = "https://api.telegram.org/bot$token/$method"

    private fun execute(request: Request, onOk: (JSONObject) -> Unit, onError: (String) -> Unit) {
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = onError(e.message ?: "No internet connection")
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string().orEmpty()
                try {
                    val json = JSONObject(body)
                    if (json.optBoolean("ok")) onOk(json.optJSONObject("result") ?: JSONObject())
                    else onError(json.optString("description", "Telegram error ${response.code}"))
                } catch (e: Exception) {
                    onError("Bad response from Telegram (${response.code})")
                }
            }
        })
    }

    private fun postForm(method: String, params: Map<String, String>, onOk: (JSONObject) -> Unit, onError: (String) -> Unit) {
        val form = FormBody.Builder().apply { params.forEach { (k, v) -> add(k, v) } }.build()
        execute(Request.Builder().url(url(method)).post(form).build(), onOk, onError)
    }

    /** Step 1 check: returns the bot's name/username. */
    fun getMe(onOk: (JSONObject) -> Unit, onError: (String) -> Unit) =
        execute(Request.Builder().url(url("getMe")).build(), onOk, onError)

    fun getChat(chatId: String, onOk: (JSONObject) -> Unit, onError: (String) -> Unit) =
        postForm("getChat", mapOf("chat_id" to chatId), onOk, onError)

    fun sendMessage(chatId: String, text: String, onOk: (JSONObject) -> Unit, onError: (String) -> Unit) =
        postForm("sendMessage", mapOf("chat_id" to chatId, "text" to text), onOk, onError)

    /** Detect the group chat ID: user sends any message in the group, then call this.
     *  NOTE: getUpdates returns a JSON ARRAY as "result" (unlike most methods,
     *  which return an object), so it cannot go through [execute]. */
    fun getUpdates(onOk: (org.json.JSONArray) -> Unit, onError: (String) -> Unit) {
        client.newCall(Request.Builder().url(url("getUpdates")).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) =
                onError(e.message ?: "No internet connection")

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string().orEmpty()
                try {
                    val json = JSONObject(body)
                    if (json.optBoolean("ok")) {
                        onOk(json.optJSONArray("result") ?: org.json.JSONArray())
                    } else {
                        onError(json.optString("description", "Telegram error ${response.code}"))
                    }
                } catch (e: Exception) {
                    onError("Bad response from Telegram (${response.code})")
                }
            }
        })
    }

    /** Upload any file (max 50 MB for bots). Uses sendDocument so the file is stored unchanged. */
    fun sendDocument(chatId: String, file: File, displayName: String, caption: String,
                     onOk: (JSONObject) -> Unit, onError: (String) -> Unit) {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("caption", caption)
            .addFormDataPart("document", displayName,
                file.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
            .build()
        execute(Request.Builder().url(url("sendDocument")).post(body).build(), onOk, onError)
    }

    fun sendPhoto(chatId: String, file: File, displayName: String, caption: String,
                  onOk: (JSONObject) -> Unit, onError: (String) -> Unit) {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("caption", caption)
            .addFormDataPart("photo", displayName,
                file.asRequestBody("image/*".toMediaTypeOrNull()))
            .build()
        execute(Request.Builder().url(url("sendPhoto")).post(body).build(), onOk, onError)
    }

    /** getFile -> file_path, then the download URL is https://api.telegram.org/file/bot<TOKEN>/<file_path> */
    fun getFile(fileId: String, onOk: (filePath: String, downloadUrl: String) -> Unit, onError: (String) -> Unit) {
        postForm("getFile", mapOf("file_id" to fileId),
            { result ->
                val path = result.optString("file_path")
                if (path.isBlank()) onError("Telegram did not return a file path")
                else onOk(path, "https://api.telegram.org/file/bot$token/$path")
            }, onError)
    }

    fun deleteMessage(chatId: String, messageId: Int, onOk: () -> Unit, onError: (String) -> Unit) =
        postForm("deleteMessage", mapOf("chat_id" to chatId, "message_id" to messageId.toString()),
            { onOk() }, onError)

    companion object {
        /** Extract the best file_id from a sendDocument/sendPhoto result message. */
        fun extractFileId(message: JSONObject): Pair<String, String> {
            message.optJSONObject("document")?.let { return it.optString("file_id") to "document" }
            message.optJSONArray("photo")?.let { arr ->
                if (arr.length() > 0) {
                    val largest = arr.getJSONObject(arr.length() - 1)
                    return largest.optString("file_id") to "photo"
                }
            }
            message.optJSONObject("video")?.let { return it.optString("file_id") to "video" }
            message.optJSONObject("audio")?.let { return it.optString("file_id") to "audio" }
            message.optJSONObject("voice")?.let { return it.optString("file_id") to "audio" }
            return "" to "document"
        }

        /** Find a group/supergroup/channel chat in getUpdates results. */
        fun findGroupChatId(updates: org.json.JSONArray): String? {
            for (i in updates.length() - 1 downTo 0) {
                val u = updates.optJSONObject(i) ?: continue
                val msg = u.optJSONObject("message") ?: u.optJSONObject("my_chat_member")
                    ?: u.optJSONObject("chat_member") ?: u.optJSONObject("edited_message")
                    ?: u.optJSONObject("channel_post") ?: continue
                val chat = msg.optJSONObject("chat") ?: continue
                val type = chat.optString("type")
                if (type == "group" || type == "supergroup" || type == "channel") {
                    return chat.optLong("id").toString()
                }
            }
            return null
        }

        fun jsonBody(text: String): okhttp3.RequestBody =
            text.toRequestBody("application/json".toMediaTypeOrNull())
    }
}
