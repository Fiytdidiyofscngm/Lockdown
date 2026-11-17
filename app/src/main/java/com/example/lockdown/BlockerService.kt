package com.example.lockdown

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class BlockerService : AccessibilityService() {
    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName || !LockStore.active(this)) return

        // Blocked apps, plus Settings and the installer so Lockdown can't be uninstalled or disabled.
        val guarded = pkg in LockStore.blocked(this) ||
            pkg == "com.android.settings" ||
            pkg.contains("packageinstaller")
        if (guarded) {
            startActivity(Intent(this, LockScreenActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
    }

    override fun onInterrupt() {}
}
