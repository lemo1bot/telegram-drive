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
    val noteText: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("kind", kind)
        put("sizeBytes", sizeBytes); put("folder", folder)
        put("telegramFileId", telegramFileId); put("telegramMessageId", telegramMessageId)
        put("uploadedAt", uploadedAt); put("noteText", noteText)
    }
    companion object {
        fun fromJson(j: JSONObject) = DriveFile(
            id = j.optString("id"), name = j.optString("name"), kind = j.optString("kind", "document"),
            sizeBytes = j.optLong("sizeBytes"), folder = j.optString("folder", "Home"),
            telegramFileId = j.optString("telegramFileId"), telegramMessageId = j.optInt("telegramMessageId"),
            uploadedAt = j.optLong("uploadedAt"), noteText = j.optString("noteText")
        )
    }
}
