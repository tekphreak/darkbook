package com.tekphreak.darkbook.ui

/**
 * Suppresses MainActivity's inactivity relock check across a single
 * onStop/onResume round-trip caused by an activity this app itself launched
 * (the system Photo Picker, a runtime permission prompt) rather than the
 * user actually backgrounding the app. Without this, picking a photo that
 * takes longer than the lock grace period silently discards the
 * in-progress entry: MainActivity forces `screen` back to Lock, unmounting
 * EntryEditScreen before the picker's result even arrives.
 */
object RelockGuard {
    @Volatile
    private var suppressed = false

    fun suppressNextRelock() {
        suppressed = true
    }

    /** Reads and clears the flag — always call this on every onResume, so a stale
     *  suppression from an unrelated earlier launch can't linger into a later one. */
    fun consumeSuppress(): Boolean {
        val wasSuppressed = suppressed
        suppressed = false
        return wasSuppressed
    }
}
