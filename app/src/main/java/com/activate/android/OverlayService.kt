package com.activate.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    companion object {
        const val ACTION_START = "com.activate.android.ACTION_START"
        const val ACTION_STOP = "com.activate.android.ACTION_STOP"
        const val ACTION_UPDATE = "com.activate.android.ACTION_UPDATE"

        const val CHANNEL_ID = "activate_android_overlay_channel"
        const val NOTIFICATION_ID = 1001

        const val PREFS_NAME = "activate_android_prefs"
        const val KEY_IS_ENABLED = "is_enabled"
        const val KEY_TITLE = "title"
        const val KEY_SUBTITLE = "subtitle"
        const val KEY_OPACITY = "opacity"
        const val KEY_MARGIN_BOTTOM = "margin_bottom"
        const val KEY_MARGIN_END = "margin_end"

        var isRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE -> {
                updateOverlayFromPrefs()
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                isRunning = true
                showOverlay()
            }
        }
        return START_STICKY
    }

    private fun showOverlay() {
        if (overlayView != null) {
            updateOverlayFromPrefs()
            return
        }

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val marginBottomDp = prefs.getInt(KEY_MARGIN_BOTTOM, 48)
        val marginEndDp = prefs.getInt(KEY_MARGIN_END, 24)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // FLAG_NOT_TOUCHABLE allows taps to pass completely through the watermark to apps underneath
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = (marginEndDp * resources.displayMetrics.density).toInt()
            y = (marginBottomDp * resources.displayMetrics.density).toInt()
        }
        layoutParams = params

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_watermark, null)
        overlayView = view

        applyPrefsToView(view)

        try {
            windowManager?.addView(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateOverlayFromPrefs() {
        val view = overlayView ?: return
        val params = layoutParams ?: return
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val marginBottomDp = prefs.getInt(KEY_MARGIN_BOTTOM, 48)
        val marginEndDp = prefs.getInt(KEY_MARGIN_END, 24)

        params.x = (marginEndDp * resources.displayMetrics.density).toInt()
        params.y = (marginBottomDp * resources.displayMetrics.density).toInt()

        applyPrefsToView(view)

        try {
            windowManager?.updateViewLayout(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applyPrefsToView(view: View) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val titleText = prefs.getString(KEY_TITLE, getString(R.string.default_title))
        val subtitleText = prefs.getString(KEY_SUBTITLE, getString(R.string.default_subtitle))
        val opacityPercent = prefs.getInt(KEY_OPACITY, 70)

        val tvTitle = view.findViewById<TextView>(R.id.tv_title)
        val tvSubtitle = view.findViewById<TextView>(R.id.tv_subtitle)

        tvTitle.text = titleText
        tvSubtitle.text = subtitleText

        val alphaFloat = (opacityPercent / 100f).coerceIn(0.1f, 1.0f)
        view.alpha = alphaFloat
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Watermark active. Tap to configure.")
            .setSmallIcon(R.drawable.ic_watermark_tile)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, "Dismiss", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Activate Android Watermark",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the watermark overlay active in background"
                enableLights(false)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
