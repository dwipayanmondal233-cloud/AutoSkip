package com.dwipayan.autoskip

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.dwipayan.autoskip.databinding.ActivityMainBinding
import com.google.android.material.chip.Chip

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val prefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == Prefs.KEY_COUNT) {
                runOnUiThread { showCount() }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOpenSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.switchEnabled.isChecked = Prefs.isEnabled(this)
        binding.switchEnabled.setOnCheckedChangeListener { _, checked ->
            Prefs.setEnabled(this, checked)
        }

        binding.btnReset.setOnClickListener {
            Prefs.setCount(this, 0)
            showCount()
        }

        binding.btnAddKeyword.setOnClickListener { addKeyword() }
        binding.etKeyword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addKeyword()
                true
            } else {
                false
            }
        }

        binding.btnDefaults.setOnClickListener {
            Prefs.setKeywords(this, Prefs.DEFAULT_KEYWORDS)
            showKeywords()
        }

        showKeywords()
    }

    override fun onStart() {
        super.onStart()
        Prefs.get(this).registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onStop() {
        super.onStop()
        Prefs.get(this).unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onResume() {
        super.onResume()
        showStatus()
        showCount()
    }

    private fun addKeyword() {
        val word = binding.etKeyword.text?.toString()?.trim().orEmpty()
        if (word.isEmpty()) {
            Toast.makeText(this, R.string.msg_empty, Toast.LENGTH_SHORT).show()
            return
        }
        val list = Prefs.getKeywords(this).toMutableList()
        if (list.any { it.equals(word, ignoreCase = true) }) {
            Toast.makeText(this, R.string.msg_duplicate, Toast.LENGTH_SHORT).show()
            return
        }
        list.add(word)
        Prefs.setKeywords(this, list)
        binding.etKeyword.setText("")
        showKeywords()
    }

    private fun deleteKeyword(word: String) {
        val list = Prefs.getKeywords(this).toMutableList()
        list.remove(word)
        Prefs.setKeywords(this, list)
        showKeywords()
    }

    private fun showKeywords() {
        val cyan = ContextCompat.getColor(this, R.color.neon_cyan)
        val dark = ContextCompat.getColor(this, R.color.bg_black)
        val group = binding.chipGroupKeywords
        group.removeAllViews()
        for (word in Prefs.getKeywords(this)) {
            val chip = Chip(this)
            chip.text = word
            chip.setTextColor(cyan)
            chip.chipBackgroundColor = ColorStateList.valueOf(dark)
            chip.chipStrokeColor = ColorStateList.valueOf(cyan)
            chip.chipStrokeWidth = 2f
            chip.isCloseIconVisible = true
            chip.closeIconTint = ColorStateList.valueOf(cyan)
            chip.setOnCloseIconClickListener { deleteKeyword(word) }
            group.addView(chip)
        }
    }

    private fun showCount() {
        binding.tvCount.text = Prefs.getCount(this).toString()
    }

    private fun showStatus() {
        if (isServiceEnabled()) {
            binding.tvStatus.text = getString(R.string.status_active)
            binding.tvStatus.setTextColor(Color.parseColor("#00FF9C"))
            binding.tvStatusHint.text = getString(R.string.status_hint_active)
        } else {
            binding.tvStatus.text = getString(R.string.status_inactive)
            binding.tvStatus.setTextColor(Color.parseColor("#FF4D6D"))
            binding.tvStatusHint.text = getString(R.string.status_hint_inactive)
        }
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val mine = ComponentName(packageName, "$packageName.AutoSkipAccessibilityService")
        return enabled.split(':').any {
            ComponentName.unflattenFromString(it) == mine
        }
    }
}