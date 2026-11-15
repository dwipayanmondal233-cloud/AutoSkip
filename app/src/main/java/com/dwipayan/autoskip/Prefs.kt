package com.dwipayan.autoskip

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    const val FILE = "autoskip_prefs"
    const val KEY_ENABLED = "enabled"
    const val KEY_COUNT = "skip_count"

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
}