package com.dwipayan.autoskip

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.dwipayan.autoskip.databinding.ActivityMainBinding

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