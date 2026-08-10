package com.operit.pet

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
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView

class PetOverlayService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var webView: WebView
    private var params: WindowManager.LayoutParams? = null

    private var downX = 0f
    private var downY = 0f
    private var origX = 0
    private var origY = 0
    private var moved = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createChannel()
        startForeground(1, buildNotification())

        webView = WebView(this)
        webView.setBackgroundColor(Color.TRANSPARENT)
        val ws: WebSettings = webView.settings
        ws.javaScriptEnabled = true
        ws.allowFileAccess = true
        ws.allowContentAccess = true
        ws.setSupportZoom(false)
        ws.domStorageEnabled = true
        webView.loadUrl("file:///android_asset/pet.html")

        val size = (resources.displayMetrics.density * 72).toInt()

        params = WindowManager.LayoutParams(
            size,
            size,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 60
            y = 260
        }

        webView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    origX = params!!.x
                    origY = params!!.y
                    moved = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (kotlin.math.abs(dx) > 5 || kotlin.math.abs(dy) > 5) {
                        moved = true
                        params!!.x = origX + dx
                        params!!.y = origY + dy
                        wm.updateViewLayout(webView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) {
                        true
                    } else {
                        val relX = (event.x / size).toDouble().coerceIn(0.0, 1.0)
                        val relY = (event.y / size).toDouble().coerceIn(0.0, 1.0)
                        webView.loadUrl("javascript:tapPet($relX,$relY)")
                        true
                    }
                }
                else -> false
            }
        }

        wm.addView(webView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        webView?.let { wm.removeView(it) }
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            "pet_channel",
            "X-YOU",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, "pet_channel")
            .setContentTitle("X-YOU在桌面上陪你")
            .setContentText("点一下它，它会冲你笑")
            .setSmallIcon(R.drawable.ic_pet_small)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }
}