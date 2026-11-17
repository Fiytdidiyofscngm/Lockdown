package com.example.lockdown

import android.app.Activity
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())

    override fun onResume() { super.onResume(); build() }
    override fun onPause() { super.onPause(); h.removeCallbacksAndMessages(null) }

    private fun build() {
        if (LockStore.active(this)) { showCountdown(); return }

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 48, 32, 32) }
        root.addView(TextView(this).apply { text = "Lockdown"; textSize = 26f })

        if (!a11yOn()) root.addView(Button(this).apply {
            text = "1. Enable accessibility service"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, AdminReceiver::class.java)
        if (!dpm.isAdminActive(admin)) root.addView(Button(this).apply {
            text = "2. Enable uninstall protection"
            setOnClickListener {
                startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin))
            }
        })

        val pm = packageManager
        val apps = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { it.first != packageName }
            .sortedBy { it.second.lowercase() }

        val list = ListView(this).apply {
            choiceMode = ListView.CHOICE_MODE_MULTIPLE
            adapter = ArrayAdapter(this@MainActivity,
                android.R.layout.simple_list_item_multiple_choice, apps.map { it.second })
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(list)

        val mins = EditText(this).apply {
            hint = "Lock duration in minutes"; inputType = InputType.TYPE_CLASS_NUMBER
        }
        root.addView(mins)
        root.addView(Button(this).apply {
            text = "Start lock"
            setOnClickListener {
                val m = mins.text.toString().toLongOrNull() ?: 0
                val picked = apps.filterIndexed { i, _ -> list.isItemChecked(i) }.map { it.first }.toSet()
                when {
                    picked.isEmpty() || m <= 0 -> toast("Pick apps and a duration")
                    !a11yOn() || !dpm.isAdminActive(admin) -> toast("Finish steps 1 and 2 first")
                    else -> AlertDialog.Builder(this@MainActivity)
                        .setTitle("This can't be undone")
                        .setMessage("${picked.size} apps locked for $m minutes. You won't be able to stop it.")
                        .setPositiveButton("Lock") { _, _ -> LockStore.start(this@MainActivity, picked, m); build() }
                        .setNegativeButton("Cancel", null).show()
                }
            }
        })
        setContentView(root)
    }

    private fun showCountdown() {
        val tv = TextView(this).apply { textSize = 30f; gravity = android.view.Gravity.CENTER }
        setContentView(tv)
        h.post(object : Runnable {
            override fun run() {
                val r = LockStore.remainingMs(this@MainActivity)
                if (r <= 0) { build(); return }
                tv.text = "🔒 Lock active\n\n" + LockScreenActivity.fmt(r)
                h.postDelayed(this, 1000)
            }
        })
    }

    private fun a11yOn(): Boolean {
        val s = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return s.contains("$packageName/${BlockerService::class.java.name}")
    }

    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_SHORT).show()
}
