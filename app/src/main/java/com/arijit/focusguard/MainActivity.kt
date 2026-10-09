package com.arijit.focusguard

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var timerText: TextView
    private lateinit var accBtn: Button
    private lateinit var startBtn: Button
    private lateinit var minutes: EditText
    private lateinit var strict: CheckBox
    private val boxes = mutableListOf<Pair<CheckBox, String>>()

    private val ticker = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 1000)
        }
    }

    // Pre-selected on first run only
    private val defaultDistractions = setOf(
        "com.instagram.android", "com.google.android.youtube", "com.facebook.katana",
        "com.snapchat.android", "com.twitter.android", "com.whatsapp",
        "org.telegram.messenger", "com.reddit.frontpage", "com.zhiliaoapp.musically",
        "in.mohalla.sharechat", "com.netflix.mediaclient", "com.discord",
        "com.pinterest", "com.linkedin.android", "com.facebook.orca"
    )

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(28), dp(16), dp(16))
        }

        root.addView(TextView(this).apply {
            text = "🛡️ Focus Guard"
            textSize = 26f
        })

        status = TextView(this).apply {
            textSize = 14f
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(status)

        accBtn = Button(this).apply {
            text = "Open Accessibility Settings"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        root.addView(accBtn)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(TextView(this).apply { text = "Focus for (minutes): " })
        minutes = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText("60")
            minEms = 4
        }
        row.addView(minutes)
        root.addView(row)

        strict = CheckBox(this).apply {
            text = "Strict mode (also blocks Settings & installer — can't cheat)"
        }
        root.addView(strict)

        startBtn = Button(this).apply {
            text = "🔒 Start focus session"
            setOnClickListener { onStartClicked() }
        }
        root.addView(startBtn)

        timerText = TextView(this).apply {
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(10))
        }
        root.addView(timerText)

        root.addView(TextView(this).apply {
            text = "Apps to block:"
            textSize = 16f
        })

        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(list) }
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        // Build app list
        val saved = Prefs.blocked(this)
        val initial = saved ?: defaultDistractions
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val protectedPkgs = Prefs.protectedPackages(this)
        val apps = packageManager.queryIntentActivities(launcherIntent, 0)
            .map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }
            .filter { it.first !in protectedPkgs }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }

        for ((pkg, label) in apps) {
            val cb = CheckBox(this).apply {
                text = label
                isChecked = pkg in initial
                setOnCheckedChangeListener { _, _ -> saveSelection() }
            }
            boxes.add(cb to pkg)
            list.addView(cb)
        }
        if (saved == null) saveSelection()

        strict.isChecked = Prefs.strict(this)
        setContentView(root)
    }

    private fun saveSelection() {
        Prefs.saveBlocked(this, boxes.filter { it.first.isChecked }.map { it.second }.toSet())
    }

    private fun onStartClicked() {
        val m = minutes.text.toString().toIntOrNull() ?: 0
        if (m !in 1..720) {
            Toast.makeText(this, "Enter 1 to 720 minutes", Toast.LENGTH_SHORT).show()
            return
        }
        if (boxes.none { it.first.isChecked }) {
            Toast.makeText(this, "Select at least one app", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Start $m min session?")
            .setMessage("You will NOT be able to undo this until the timer ends. Sure?")
            .setPositiveButton("Lock in 💪") { _, _ ->
                saveSelection()
                Prefs.start(this, System.currentTimeMillis() + m * 60_000L, strict.isChecked)
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val cn = ComponentName(this, BlockerService::class.java)
        val full = cn.flattenToString()
        val short = cn.flattenToShortString()
        return enabled.split(':').any { it.equals(full, true) || it.equals(short, true) }
    }

    private fun refresh() {
        val left = Prefs.endTime(this) - System.currentTimeMillis()
        val active = left > 0
        val enabled = isServiceEnabled()

        accBtn.visibility = if (enabled) View.GONE else View.VISIBLE
        status.text = if (enabled) "✅ Blocker service is ON"
        else "⚠️ Turn on \"Focus Guard\" in Accessibility settings (see README if it's greyed out)"

        if (active) {
            val s = left / 1000
            timerText.text = String.format("🔥 %02d:%02d:%02d left", s / 3600, (s % 3600) / 60, s % 60)
        } else {
            timerText.text = "No active session"
        }

        startBtn.isEnabled = !active && enabled
        minutes.isEnabled = !active
        strict.isEnabled = !active
        boxes.forEach { it.first.isEnabled = !active }
    }

    override fun onResume() {
        super.onResume()
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }
}
