package com.foriad.telegramdrive

import org.json.JSONObject

data class DriveFile(
    val id: String,
    val name: String,
    val kind: String, // document, photo, video, audio, note, link
    val sizeBytes: Long,
    val folder: String,
    val telegramFileId: String,
    val telegramMessageId: Int,
    val uploadedAt: Long,
    val noteText: String = "",
    // Large files are split into <=19 MB parts (Telegram bot limits:
    // 50 MB send / 20 MB download per file). Empty = single-file upload.
    val parts: List<String> = emptyList(),      // Telegram file_id of each part, in order
    val partMsgs: List<Int> = emptyList()       // Telegram message_id of each part, in order
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("kind", kind)
        put("sizeBytes", sizeBytes); put("folder", folder)
        put("telegramFileId", telegramFileId); put("telegramMessageId", telegramMessageId)
        put("uploadedAt", uploadedAt); put("noteText", noteText)
        put("parts", org.json.JSONArray(parts)); put("partMsgs", org.json.JSONArray(partMsgs))
    }
    companion object {
        fun fromJson(j: JSONObject) = DriveFile(
            id = j.optString("id"), name = j.optString("name"), kind = j.optString("kind", "document"),
            sizeBytes = j.optLong("sizeBytes"), folder = j.optString("folder", "Home"),
            telegramFileId = j.optString("telegramFileId"), telegramMessageId = j.optInt("telegramMessageId"),
            uploadedAt = j.optLong("uploadedAt"), noteText = j.optString("noteText"),
            parts = List(j.optJSONArray("parts")?.length() ?: 0) { j.optJSONArray("parts")!!.optString(it) },
            partMsgs = List(j.optJSONArray("partMsgs")?.length() ?: 0) { j.optJSONArray("partMsgs")!!.optInt(it) }
        )
    }
}
