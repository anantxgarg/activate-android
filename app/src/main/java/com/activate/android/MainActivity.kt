package com.activate.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.activate.android.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updatePermissionUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadPreferences()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionUI()
        syncSwitchWithServiceState()
    }

    private fun setupUI() {
        // Permission Button
        binding.btnGrantPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            }
        }

        // Overlay Toggle Switch
        binding.switchOverlay.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (hasOverlayPermission()) {
                    startOverlayService()
                } else {
                    binding.switchOverlay.isChecked = false
                    Toast.makeText(this, "Grant overlay permission first", Toast.LENGTH_SHORT).show()
                }
            } else {
                stopOverlayService()
            }
            saveBoolean(OverlayService.KEY_IS_ENABLED, isChecked)
        }

        // Text Watchers for Live Preview & Live Update
        binding.etTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString() ?: ""
                binding.previewTitle.text = text
                saveString(OverlayService.KEY_TITLE, text)
                notifyServiceUpdate()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etSubtitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString() ?: ""
                binding.previewSubtitle.text = text
                saveString(OverlayService.KEY_SUBTITLE, text)
                notifyServiceUpdate()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Opacity Slider
        binding.sliderOpacity.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val intVal = value.toInt()
                binding.tvOpacityLabel.text = "Opacity: $intVal%"
                binding.previewContainer.alpha = (intVal / 100f).coerceIn(0.1f, 1f)
                saveInt(OverlayService.KEY_OPACITY, intVal)
                notifyServiceUpdate()
            }
        }

        // Margins Sliders
        binding.sliderMarginBottom.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val intVal = value.toInt()
                binding.tvMarginBottomLabel.text = "Bottom Offset: ${intVal}dp"
                saveInt(OverlayService.KEY_MARGIN_BOTTOM, intVal)
                notifyServiceUpdate()
            }
        }

        binding.sliderMarginEnd.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val intVal = value.toInt()
                binding.tvMarginEndLabel.text = "Right Offset: ${intVal}dp"
                saveInt(OverlayService.KEY_MARGIN_END, intVal)
                notifyServiceUpdate()
            }
        }

        // Lockscreen Watermark Checkbox
        binding.cbLockscreen.setOnCheckedChangeListener { _, isChecked ->
            saveBoolean(OverlayService.KEY_SHOW_ON_LOCKSCREEN, isChecked)
            notifyServiceUpdate()
        }

        // Start on Boot Checkbox
        binding.cbBoot.setOnCheckedChangeListener { _, isChecked ->
            saveBoolean("start_on_boot", isChecked)
        }

        // Reset to Defaults Button
        binding.btnResetDefaults.setOnClickListener {
            resetToDefaults()
        }
    }

    private fun resetToDefaults() {
        val defaultTitle = getString(R.string.default_title)
        val defaultSubtitle = getString(R.string.default_subtitle)
        val defaultOpacity = 70
        val defaultMarginBottom = 48
        val defaultMarginEnd = 24
        val defaultLockscreen = true

        binding.etTitle.setText(defaultTitle)
        binding.etSubtitle.setText(defaultSubtitle)
        binding.previewTitle.text = defaultTitle
        binding.previewSubtitle.text = defaultSubtitle

        binding.sliderOpacity.value = defaultOpacity.toFloat()
        binding.tvOpacityLabel.text = "Opacity: $defaultOpacity%"
        binding.previewContainer.alpha = (defaultOpacity / 100f).coerceIn(0.1f, 1f)

        binding.sliderMarginBottom.value = defaultMarginBottom.toFloat()
        binding.tvMarginBottomLabel.text = "Bottom Offset: ${defaultMarginBottom}dp"

        binding.sliderMarginEnd.value = defaultMarginEnd.toFloat()
        binding.tvMarginEndLabel.text = "Right Offset: ${defaultMarginEnd}dp"

        binding.cbLockscreen.isChecked = defaultLockscreen

        getSharedPreferences(OverlayService.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(OverlayService.KEY_TITLE, defaultTitle)
            .putString(OverlayService.KEY_SUBTITLE, defaultSubtitle)
            .putInt(OverlayService.KEY_OPACITY, defaultOpacity)
            .putInt(OverlayService.KEY_MARGIN_BOTTOM, defaultMarginBottom)
            .putInt(OverlayService.KEY_MARGIN_END, defaultMarginEnd)
            .putBoolean(OverlayService.KEY_SHOW_ON_LOCKSCREEN, defaultLockscreen)
            .apply()

        notifyServiceUpdate()
        Toast.makeText(this, "Reset to default settings", Toast.LENGTH_SHORT).show()
    }

    private fun loadPreferences() {
        val prefs = getSharedPreferences(OverlayService.PREFS_NAME, Context.MODE_PRIVATE)

        val title = prefs.getString(OverlayService.KEY_TITLE, getString(R.string.default_title)) ?: ""
        val subtitle = prefs.getString(OverlayService.KEY_SUBTITLE, getString(R.string.default_subtitle)) ?: ""
        val opacity = prefs.getInt(OverlayService.KEY_OPACITY, 70)
        val marginBottom = prefs.getInt(OverlayService.KEY_MARGIN_BOTTOM, 48)
        val marginEnd = prefs.getInt(OverlayService.KEY_MARGIN_END, 24)
        val showOnLockscreen = prefs.getBoolean(OverlayService.KEY_SHOW_ON_LOCKSCREEN, true)
        val startOnBoot = prefs.getBoolean("start_on_boot", false)

        binding.etTitle.setText(title)
        binding.etSubtitle.setText(subtitle)
        binding.previewTitle.text = title
        binding.previewSubtitle.text = subtitle

        binding.sliderOpacity.value = opacity.toFloat()
        binding.tvOpacityLabel.text = "Opacity: $opacity%"
        binding.previewContainer.alpha = (opacity / 100f).coerceIn(0.1f, 1f)

        binding.sliderMarginBottom.value = marginBottom.toFloat()
        binding.tvMarginBottomLabel.text = "Bottom Offset: ${marginBottom}dp"

        binding.sliderMarginEnd.value = marginEnd.toFloat()
        binding.tvMarginEndLabel.text = "Right Offset: ${marginEnd}dp"

        binding.cbLockscreen.isChecked = showOnLockscreen
        binding.cbBoot.isChecked = startOnBoot
    }

    private fun updatePermissionUI() {
        val granted = hasOverlayPermission()
        if (granted) {
            binding.tvPermissionStatus.text = getString(R.string.permission_granted)
            binding.tvPermissionDesc.text = "All required permissions are enabled. You can now toggle the overlay."
            binding.btnGrantPermission.isEnabled = false
            binding.btnGrantPermission.text = "Granted"
            binding.cardPermission.strokeColor = ContextCompat.getColor(this, android.R.color.holo_green_dark)
        } else {
            binding.tvPermissionStatus.text = getString(R.string.permission_required_title)
            binding.tvPermissionDesc.text = getString(R.string.permission_required_desc)
            binding.btnGrantPermission.isEnabled = true
            binding.btnGrantPermission.text = getString(R.string.grant_permission)
        }
    }

    private fun syncSwitchWithServiceState() {
        binding.switchOverlay.isChecked = OverlayService.isRunning
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopOverlayService() {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_STOP
        }
        startService(intent)
    }

    private fun notifyServiceUpdate() {
        if (OverlayService.isRunning) {
            val intent = Intent(this, OverlayService::class.java).apply {
                action = OverlayService.ACTION_UPDATE
            }
            startService(intent)
        }
    }

    private fun saveString(key: String, value: String) {
        getSharedPreferences(OverlayService.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(key, value)
            .apply()
    }

    private fun saveInt(key: String, value: Int) {
        getSharedPreferences(OverlayService.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(key, value)
            .apply()
    }

    private fun saveBoolean(key: String, value: Boolean) {
        getSharedPreferences(OverlayService.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key, value)
            .apply()
    }
}
