package com.arijit.focusguard

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class BlockerService : AccessibilityService() {

    private var lastToast = 0L

    // Packages that are blocked additionally in "strict mode" so you can't
    // switch the service off / uninstall the app mid-session.
    private val strictPackages = setOf(
        "com.android.settings",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.samsung.android.packageinstaller",
        "com.miui.packageinstaller"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (!Prefs.isActive(this)) return

        val blocked = Prefs.blocked(this) ?: emptySet()
        val shouldBlock = pkg in blocked || (Prefs.strict(this) && pkg in strictPackages)
        if (!shouldBlock) return
        if (pkg in Prefs.protectedPackages(this)) return

        performGlobalAction(GLOBAL_ACTION_HOME)

        val now = System.currentTimeMillis()
        if (now - lastToast > 2000) {
            lastToast = now
            val minsLeft = ((Prefs.endTime(this) - now) / 60_000L) + 1
            Toast.makeText(this, "🚫 Focus mode ON — $minsLeft min left. Back to studies!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInterrupt() {}
}
