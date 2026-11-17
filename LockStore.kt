package com.example.lockdown

import android.content.Context
import android.os.SystemClock

/** Saves the lock. Uses elapsed-realtime so changing the phone clock can't shorten it. */
object LockStore {
    private fun p(c: Context) = c.getSharedPreferences("lock", Context.MODE_PRIVATE)

    fun start(c: Context, pkgs: Set<String>, minutes: Long) {
        val ms = minutes * 60_000L
        p(c).edit()
            .putStringSet("pkgs", pkgs)
            .putLong("endElapsed", SystemClock.elapsedRealtime() + ms)
            .putLong("startElapsed", SystemClock.elapsedRealtime())
            .putLong("endWall", System.currentTimeMillis() + ms)
            .apply()
    }

    fun remainingMs(c: Context): Long {
        val s = p(c)
        val now = SystemClock.elapsedRealtime()
        // If elapsed time went backwards the phone rebooted, so fall back to wall clock.
        val rem = if (now >= s.getLong("startElapsed", Long.MAX_VALUE))
            s.getLong("endElapsed", 0) - now
        else s.getLong("endWall", 0) - System.currentTimeMillis()
        return rem.coerceAtLeast(0)
    }

    fun active(c: Context) = remainingMs(c) > 0
    fun blocked(c: Context): Set<String> = p(c).getStringSet("pkgs", emptySet()) ?: emptySet()
}
