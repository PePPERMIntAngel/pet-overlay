package com.operit.pet

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)
        val btnStart = findViewById<Button>(R.id.btn_start)
        val btnStop = findViewById<Button>(R.id.btn_stop)

        btnStart.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startPet()
            } else {
                status.text = "需要悬浮窗权限，正在跳转设置……"
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, PetOverlayService::class.java))
            status.text = "蓝猫猫已收起"
        }
    }

    private fun startPet() {
        val intent = Intent(this, PetOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        status.text = "蓝猫猫已上屏幕！"
    }

    override fun onResume() {
        super.onResume()
        status.text = if (Settings.canDrawOverlays(this)) {
            "权限已就绪，点按钮让蓝猫猫上屏幕"
        } else {
            "还没获得悬浮窗权限"
        }
    }
}