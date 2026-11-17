package com.example.lockdown

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.TextView

class LockScreenActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private lateinit var tv: TextView

    private val tick = object : Runnable {
        override fun run() {
            val r = LockStore.remainingMs(this@LockScreenActivity)
            if (r <= 0) { finish(); return }
            tv.text = "🔒 Locked\n\n" + fmt(r)
            h.postDelayed(this, 1000)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        tv = TextView(this).apply {
            textSize = 28f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#111827"))
        }
        setContentView(tv)
    }

    override fun onResume() { super.onResume(); h.post(tick) }
    override fun onPause() { super.onPause(); h.removeCallbacks(tick) }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = goHome()

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    companion object {
        fun fmt(ms: Long): String {
            val s = ms / 1000
            return "%d:%02d:%02d".format(s / 3600, s % 3600 / 60, s % 60)
        }
    }
}
