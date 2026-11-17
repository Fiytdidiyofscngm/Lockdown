package com.example.lockdown

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/** While active, Android won't let the app be uninstalled until admin is turned off (and Settings is blocked during a lock). */
class AdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        "Disabling this will let Lockdown be removed."
}
