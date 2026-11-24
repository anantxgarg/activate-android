package com.activate.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LockscreenWatermarkActivity : AppCompatActivity() {

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_PRESENT) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over keyguard / lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(false)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        // Allow touches and gestures to pass directly through to lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        setContentView(R.layout.activity_lockscreen_watermark)
        applyPreferences()

        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        registerReceiver(unlockReceiver, filter)
    }

    private fun applyPreferences() {
        val prefs = getSharedPreferences(OverlayService.PREFS_NAME, Context.MODE_PRIVATE)
        val titleText = prefs.getString(OverlayService.KEY_TITLE, getString(R.string.default_title))
        val subtitleText = prefs.getString(OverlayService.KEY_SUBTITLE, getString(R.string.default_subtitle))
        val opacityPercent = prefs.getInt(OverlayService.KEY_OPACITY, 70)
        val marginBottomDp = prefs.getInt(OverlayService.KEY_MARGIN_BOTTOM, 48)
        val marginEndDp = prefs.getInt(OverlayService.KEY_MARGIN_END, 24)

        val tvTitle = findViewById<TextView>(R.id.tv_title)
        val tvSubtitle = findViewById<TextView>(R.id.tv_subtitle)
        val watermarkContainer = findViewById<View>(R.id.ll_watermark_container)

        tvTitle?.text = titleText
        tvSubtitle?.text = subtitleText

        val alphaFloat = (opacityPercent / 100f).coerceIn(0.1f, 1.0f)
        watermarkContainer?.alpha = alphaFloat

        val density = resources.displayMetrics.density
        val marginEndPx = (marginEndDp * density).toInt()
        val marginBottomPx = (marginBottomDp * density).toInt()

        val params = watermarkContainer?.layoutParams as? FrameLayout.LayoutParams
        params?.let {
            it.gravity = Gravity.BOTTOM or Gravity.END
            it.setMargins(0, 0, marginEndPx, marginBottomPx)
            watermarkContainer.layoutParams = it
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(unlockReceiver)
        } catch (e: Exception) {
            // ignore
        }
    }
}
