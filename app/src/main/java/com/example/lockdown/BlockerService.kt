package com.example.lockdown

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView

class BlockerService : AccessibilityService() {
    private val h = Handler(Looper.getMainLooper())
    private var overlay: TextView? = null

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
        super.onDestroy()
    }
}
