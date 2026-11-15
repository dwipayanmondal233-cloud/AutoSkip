package com.dwipayan.autoskip

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.SharedPreferences
import android.graphics.Path
import android.graphics.Rect
import android.os.PowerManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AutoSkipAccessibilityService : AccessibilityService() {

    companion object {
        private const val MAX_DEPTH = 40
        private const val MAX_PARENT_STEPS = 8
        private const val COOLDOWN_MS = 1000L
        private const val SCAN_GAP_MS = 150L
    }

    @Volatile
    private var keywords: List<String> = emptyList()

    private var lastClickTime = 0L
    private var lastScanTime = 0L

    private val prefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == Prefs.KEY_KEYWORDS) loadKeywords()
        }

    override fun onServiceConnected() {
        super.onServiceConnected()
        loadKeywords()
        Prefs.get(this).registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onDestroy() {
        Prefs.get(this).unregisterOnSharedPreferenceChangeListener(prefsListener)
        super.onDestroy()
    }

    override fun onInterrupt() {
    }

    private fun loadKeywords() {
        keywords = Prefs.getKeywords(this)
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return

        if (!Prefs.isEnabled(this)) return

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isInteractive) return

        if (event.packageName?.toString() == packageName) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime < COOLDOWN_MS) return
        if (now - lastScanTime < SCAN_GAP_MS) return
        lastScanTime = now

        if (keywords.isEmpty()) return

        val root = rootInActiveWindow ?: return
        val target = findMatch(root, 0)
        if (target != null) {
            val clicked = clickTarget(target)
            if (target !== root) target.recycle()
            if (clicked) {
                lastClickTime = SystemClock.elapsedRealtime()
                Prefs.setCount(this, Prefs.getCount(this) + 1)
            }
        }
        root.recycle()
    }

    private fun findMatch(node: AccessibilityNodeInfo, depth: Int): AccessibilityNodeInfo? {
        if (depth > MAX_DEPTH) return null

        if (node.isVisibleToUser && matches(node)) return node

        val count = node.childCount
        for (i in 0 until count) {
            val child = node.getChild(i) ?: continue
            val found = findMatch(child, depth + 1)
            if (found !== child) child.recycle()
            if (found != null) return found
        }
        return null
    }

    private fun matches(node: AccessibilityNodeInfo): Boolean {
        return isKeyword(node.text) || isKeyword(node.contentDescription)
    }

    private fun isKeyword(value: CharSequence?): Boolean {
        if (value == null) return false
        val text = value.toString()
            .trim()
            .trimEnd('.', '\u2026', '\u203A', '>', ' ')
            .lowercase()
        if (text.isEmpty() || text.length > 40) return false
        return keywords.contains(text)
    }

    private fun clickTarget(node: AccessibilityNodeInfo): Boolean {
        if (clickWithParents(node)) return true
        return tapAtCenter(node)
    }

    private fun clickWithParents(node: AccessibilityNodeInfo): Boolean {
        var cur: AccessibilityNodeInfo? = node
        var steps = 0
        while (cur != null && steps < MAX_PARENT_STEPS) {
            if (cur.isClickable && cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                if (cur !== node) cur.recycle()
                return true
            }
            val parent = cur.parent
            if (cur !== node) cur.recycle()
            cur = parent
            steps++
        }
        if (cur != null && cur !== node) cur.recycle()
        return false
    }

    private fun tapAtCenter(node: AccessibilityNodeInfo): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.isEmpty) return false
        val path = Path()
        path.moveTo(rect.exactCenterX(), rect.exactCenterY())
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }
}