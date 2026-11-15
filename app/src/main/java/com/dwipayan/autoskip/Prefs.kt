package com.dwipayan.autoskip

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    const val FILE = "autoskip_prefs"
    const val KEY_ENABLED = "enabled"
    const val KEY_COUNT = "skip_count"
    const val KEY_KEYWORDS = "keywords"

    val DEFAULT_KEYWORDS = listOf(
        "Skip",
        "Skip Ad",
        "Skip Ads",
        "Skip intro",
        "No thanks"
    )

    fun get(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        get(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, value: Boolean) {
        get(context).edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun getCount(context: Context): Int =
        get(context).getInt(KEY_COUNT, 0)

    fun setCount(context: Context, value: Int) {
        get(context).edit().putInt(KEY_COUNT, value).apply()
    }

    fun getKeywords(context: Context): List<String> {
        val raw = get(context).getString(KEY_KEYWORDS, null) ?: return DEFAULT_KEYWORDS
        return raw.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun setKeywords(context: Context, list: List<String>) {
        get(context).edit().putString(KEY_KEYWORDS, list.joinToString("\n")).apply()
    }
}