package com.waleed.newsrealtime

import android.content.Context

/** Small settings stored on the phone (SharedPreferences). */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("nrt_prefs", Context.MODE_PRIVATE)

    var langFilter: String
        get() = sp.getString("lang", "all") ?: "all"
        set(v) { sp.edit().putString("lang", v).apply() }

    var sortTop: Boolean
        get() = sp.getBoolean("sort_top", false)
        set(v) { sp.edit().putBoolean("sort_top", v).apply() }

    var textScale: Float
        get() = sp.getFloat("text_scale", 1.0f)
        set(v) { sp.edit().putFloat("text_scale", v).apply() }

    var notifyBreaking: Boolean
        get() = sp.getBoolean("notify_breaking", true)
        set(v) { sp.edit().putBoolean("notify_breaking", v).apply() }

    var notifyKeywords: Boolean
        get() = sp.getBoolean("notify_keywords", true)
        set(v) { sp.edit().putBoolean("notify_keywords", v).apply() }

    var digestEnabled: Boolean
        get() = sp.getBoolean("digest", true)
        set(v) { sp.edit().putBoolean("digest", v).apply() }

    var digestHour: Int
        get() = sp.getInt("digest_hour", 7)
        set(v) { sp.edit().putInt("digest_hour", v).apply() }

    var quietEnabled: Boolean
        get() = sp.getBoolean("quiet", true)
        set(v) { sp.edit().putBoolean("quiet", v).apply() }

    var quietStart: Int
        get() = sp.getInt("quiet_start", 23)
        set(v) { sp.edit().putInt("quiet_start", v).apply() }

    var quietEnd: Int
        get() = sp.getInt("quiet_end", 7)
        set(v) { sp.edit().putInt("quiet_end", v).apply() }

    var retentionDays: Int
        get() = sp.getInt("retention_days", 7)
        set(v) { sp.edit().putInt("retention_days", v).apply() }

    var claudeKey: String
        get() = sp.getString("claude_key", "") ?: ""
        set(v) { sp.edit().putString("claude_key", v).apply() }

    var claudeModel: String
        get() = sp.getString("claude_model", "claude-sonnet-5-5") ?: "claude-sonnet-5-5"
        set(v) { sp.edit().putString("claude_model", v).apply() }

    var lastRefresh: Long
        get() = sp.getLong("last_refresh", 0L)
        set(v) { sp.edit().putLong("last_refresh", v).apply() }

    var keywords: List<String>
        get() = split(sp.getString("keywords", "") ?: "")
        set(v) { sp.edit().putString("keywords", join(v)).apply() }

    var mutedWords: List<String>
        get() = split(sp.getString("muted", "") ?: "")
        set(v) { sp.edit().putString("muted", join(v)).apply() }

    private fun split(s: String): List<String> = s.split("|").map { it.trim() }.filter { it.isNotEmpty() }

    private fun join(v: List<String>): String =
        v.map { it.replace("|", " ").trim() }.filter { it.isNotEmpty() }.distinct().joinToString("|")
}
