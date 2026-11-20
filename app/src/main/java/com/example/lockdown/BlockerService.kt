package com.example.lockdown

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView

class BlockerService : AccessibilityService() {
    private val h = Handler(Looper.getMainLooper())
    private var overlay: TextView? = null
    private var bar: TextView? = null

    private val ticker = object : Runnable {
        override fun run() {
            try {
                if (LockStore.active(this@BlockerService)) {
                    showBar()
                    bar?.text = "🔒 " + LockScreenActivity.fmt(LockStore.remainingMs(this@BlockerService))
                } else {
                    hideBar()
                }
            } catch (_: Exception) {
            }
            h.postDelayed(this, 1000)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        h.removeCallbacks(ticker)
        h.post(ticker)
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        try {
            if (e == null || e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
            val pkg = e.packageName?.toString() ?: return
            if (pkg == packageName || overlay != null || !LockStore.active(this)) return

            val blocked = pkg in LockStore.blocked(this)
            val guarded = blocked || pkg == "com.android.settings" || pkg == "com.miui.securitycenter" || pkg.contains("packageinstaller")
            if (!guarded) return

            showOverlay()
            h.postDelayed({ performGlobalAction(GLOBAL_ACTION_HOME) }, 1000)
            h.postDelayed({
                if (blocked) killApp(pkg)
                hideOverlay()
            }, 1500)
        } catch (_: Exception) {
        }
    }

    private fun showBar() {
        if (bar != null) return
        val tv = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(32, 8, 32, 8)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CC111827"))
                cornerRadius = 48f
            }
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL }
        (getSystemService(WINDOW_SERVICE) as WindowManager).addView(tv, lp)
        bar = tv
    }

    private fun hideBar() {
        bar?.let {
            try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) } catch (_: Exception) {}
        }
        bar = null
    }

    private fun showOverlay() {
        val tv = TextView(this).apply {
            text = "🔒 Locked\n\n" + LockScreenActivity.fmt(LockStore.remainingMs(this@BlockerService))
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#111827"))
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE
        )
        (getSystemService(WINDOW_SERVICE) as WindowManager).addView(tv, lp)
        overlay = tv
    }

    private fun hideOverlay() {
        overlay?.let {
            try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) } catch (_: Exception) {}
        }
        overlay = null
    }

    private fun killApp(pkg: String) {
        try { (getSystemService(ACTIVITY_SERVICE) as ActivityManager).killBackgroundProcesses(pkg) } catch (_: Exception) {}
    }

    override fun onInterrupt() { hideOverlay() }

    override fun onDestroy() {
        h.removeCallbacksAndMessages(null)
        hideOverlay()
        hideBar()
        super.onDestroy()
    }
}
