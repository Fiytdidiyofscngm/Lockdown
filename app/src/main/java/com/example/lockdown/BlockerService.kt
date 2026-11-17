package com.example.lockdown

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class BlockerService : AccessibilityService() {
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        try {
            if (e == null || e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
            val pkg = e.packageName?.toString() ?: return
            if (pkg == packageName || !LockStore.active(this)) return

            val guarded = pkg in LockStore.blocked(this) ||
                pkg == "com.android.settings" ||
                pkg.contains("packageinstaller")
            if (guarded) {
                performGlobalAction(GLOBAL_ACTION_HOME)
                startActivity(Intent(this, LockScreenActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
            }
        } catch (_: Exception) {
        }
    }

    override fun onInterrupt() {}
}
