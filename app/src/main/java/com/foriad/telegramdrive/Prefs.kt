package com.foriad.telegramdrive

import android.content.Context
import org.json.JSONArray

/** Local-only storage. Token and chat ID never leave the device except to api.telegram.org. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("telegram_drive", Context.MODE_PRIVATE)

    var botToken: String
        get() = sp.getString("bot_token", "") ?: ""
        set(v) = sp.edit().putString("bot_token", v).apply()

    var chatId: String
        get() = sp.getString("chat_id", "") ?: ""
        set(v) = sp.edit().putString("chat_id", v).apply()

    var botName: String
        get() = sp.getString("bot_name", "") ?: ""
        set(v) = sp.edit().putString("bot_name", v).apply()

    /**
     * Premium (lifetime, $10 one-time via Google Play — see PremiumConfig /
     * BillingManager). Set to true only after Play reports a verified,
     * acknowledged purchase of `premium_lifetime`. When true, ALL ads are
     * permanently removed. Stored locally; Restore Purchase re-verifies
     * ownership with Play after a reinstall / on a new phone.
     */
    var isPremium: Boolean
        get() = sp.getBoolean("is_premium", false)
        set(v) = sp.edit().putBoolean("is_premium", v).apply()

    val isConnected: Boolean get() = botToken.isNotBlank() && chatId.isNotBlank()

    fun disconnect() = sp.edit().remove("bot_token").remove("chat_id").remove("bot_name").apply()

    fun loadFiles(): MutableList<DriveFile> {
        val raw = sp.getString("files_json", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return MutableList(arr.length()) { DriveFile.fromJson(arr.getJSONObject(it)) }
    }

    fun saveFiles(files: List<DriveFile>) {
        val arr = JSONArray()
        files.forEach { arr.put(it.toJson()) }
        sp.edit().putString("files_json", arr.toString()).apply()
    }

    fun loadFolders(): MutableList<String> {
        val raw = sp.getString("folders_json", "[\"Home\"]") ?: "[\"Home\"]"
        val arr = JSONArray(raw)
        val out = MutableList(arr.length()) { arr.optString(it) }
        if ("Home" !in out) out.add(0, "Home")
        return out
    }

    fun saveFolders(folders: List<String>) {
        val arr = JSONArray()
        folders.distinct().forEach { arr.put(it) }
        sp.edit().putString("folders_json", arr.toString()).apply()
    }
}
