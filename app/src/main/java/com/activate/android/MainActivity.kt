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

        // Preset Chips
        binding.chipAndroid.setOnClickListener { setPreset("Activate Android", "Go to Settings to activate Android.") }
        binding.chipPixel.setOnClickListener { setPreset("Activate Pixel", "Go to System to activate your Pixel.") }
        binding.chipSamsung.setOnClickListener { setPreset("Activate One UI", "Go to Settings to activate One UI.") }
        binding.chipLineage.setOnClickListener { setPreset("Activate LineageOS", "Go to Settings to activate LineageOS.") }
        binding.chipWindows.setOnClickListener { setPreset("Activate Windows", "Go to Settings to activate Windows.") }

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

        // Start on Boot Checkbox
        binding.cbBoot.setOnCheckedChangeListener { _, isChecked ->
            saveBoolean("start_on_boot", isChecked)
        }
    }

    private fun setPreset(title: String, subtitle: String) {
        binding.etTitle.setText(title)
        binding.etSubtitle.setText(subtitle)
    }

    private fun loadPreferences() {
        val prefs = getSharedPreferences(OverlayService.PREFS_NAME, Context.MODE_PRIVATE)

        val title = prefs.getString(OverlayService.KEY_TITLE, getString(R.string.default_title)) ?: ""
        val subtitle = prefs.getString(OverlayService.KEY_SUBTITLE, getString(R.string.default_subtitle)) ?: ""
        val opacity = prefs.getInt(OverlayService.KEY_OPACITY, 70)
        val marginBottom = prefs.getInt(OverlayService.KEY_MARGIN_BOTTOM, 48)
        val marginEnd = prefs.getInt(OverlayService.KEY_MARGIN_END, 24)
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
