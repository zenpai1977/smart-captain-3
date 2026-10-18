package com.smartcaptain.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.SharedPreferences
import java.util.Locale

/**
 * Checks text only in configured driver-app package IDs. A tap is attempted only when
 * consent is enabled, filters are on, the cloud pass is active, all required offer values
 * are parsed, every rule matches, and a labelled enabled Accept control exists.
 * A tap attempt does not prove the ride was accepted; verify the result in the driver app.
 */
class RideAccessibilityService : AccessibilityService() {
    companion object {
        const val PREFS = "smart_captain"
        const val KEY_CONSENT = "consent"
        const val KEY_AUTO_ACCEPT = "auto_accept"
        const val KEY_PASS_ACTIVE = "pass_active"
        const val KEY_FILTERS_ENABLED = "filters_enabled"
        const val KEY_MIN_FARE = "min_fare"
        const val KEY_MAX_PICKUP = "max_pickup_km"
        const val KEY_MIN_TRIP = "min_trip_km"
        const val KEY_DESTINATION = "destination_keyword"
        const val KEY_PLATFORMS = "enabled_platforms"
        const val KEY_VEHICLE_TYPES = "enabled_vehicle_types"
        const val KEY_PACKAGE_MAP = "package_map"
        const val KEY_LAST_PACKAGE = "last_package"
        const val KEY_LAST_TEXT = "last_text"
        const val KEY_LAST_DECISION = "last_decision"
        const val KEY_LAST_TIME = "last_time"
        const val KEY_LAST_ACTION = "last_action"
        const val KEY_PHONE = "driver_phone"

        const val DEFAULT_PACKAGE_MAP = "com.rapido.rider=Rapido\n" +
            "com.ubercab.driver=Uber\n" +
            "com.ubercab.partner=Uber\n" +
            "com.theporter.android.driverapp=Porter"

        private val ACCEPT_LABELS = setOf(
            "accept", "accept ride", "accept trip", "accept order", "accept booking",
            "accept now", "take ride", "take trip", "take order",
            "स्वीकार करें", "राइड स्वीकार करें", "ऑर्डर स्वीकार करें"
        )
        private val ACCEPT_WITH_VALUE = Regex("""(?i)^accept(?:\s+(?:ride|trip|order|booking|now))?\s*(?:₹\s*[\d,.]+)?$""")
    }

    private var lastAttemptFingerprint: Int? = null
    private var lastAttemptAt: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_CONSENT, false) ||
            !prefs.getBoolean(KEY_FILTERS_ENABLED, true)) return

        val packageName = event.packageName?.toString() ?: return
        val platform = parsePackageMap(
            prefs.getString(KEY_PACKAGE_MAP, DEFAULT_PACKAGE_MAP) ?: DEFAULT_PACKAGE_MAP
        )[packageName] ?: return
        val root = rootInActiveWindow ?: return

        try {
            val lines = mutableListOf<String>()
            collectVisibleText(root, lines, 0)
            val texts = lines.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            if (texts.isEmpty()) return
            val screenText = texts.joinToString(" | ").take(2600)
            val parsed = RideOfferParser.parse(platform, texts)
            val now = System.currentTimeMillis()

            prefs.edit()
                .putString(KEY_LAST_PACKAGE, "$platform ($packageName)")
                .putString(KEY_LAST_TEXT, screenText)
                .putLong(KEY_LAST_TIME, now)
                .apply()

            val offer = parsed.offer
            if (offer == null) {
                prefs.edit()
                    .putString(KEY_LAST_ACTION, "No click: offer was not parsed")
                    .putString(KEY_LAST_DECISION, "No safe offer parsed. Missing labelled field(s): ${parsed.missingFields.joinToString(", ")}.")
                    .apply()
                return
            }

            val decision = RideRuleEngine.evaluate(loadRules(prefs), offer)
            val summary = "Offer: ${offer.platform} · ₹${offer.fare} · pickup ${offer.pickupKm} km · trip ${offer.tripKm} km" +
                (if (offer.destination.isNotBlank()) " · to ${offer.destination}" else "") +
                (if (offer.vehicleType != null) " · ${offer.vehicleType}" else "")

            if (!decision.matches) {
                prefs.edit()
                    .putString(KEY_LAST_ACTION, "No click: rules not matched")
                    .putString(KEY_LAST_DECISION, "$summary\nNOT MATCHED: ${decision.reasons.joinToString("; ")}")
                    .apply()
                return
            }
            if (!prefs.getBoolean(KEY_PASS_ACTIVE, false)) {
                prefs.edit().putString(KEY_LAST_ACTION, "No click: cloud pass not active")
                    .putString(KEY_LAST_DECISION, "$summary\nMATCHED, but cloud pass is not active.").apply()
                return
            }
            if (!prefs.getBoolean(KEY_AUTO_ACCEPT, false)) {
                prefs.edit().putString(KEY_LAST_ACTION, "No click: auto-accept OFF")
                    .putString(KEY_LAST_DECISION, "$summary\nMATCHED. Auto-accept is OFF; no button was pressed.").apply()
                return
            }

            val fingerprint = (packageName + "|" + texts.joinToString("|")).hashCode()
            if (lastAttemptFingerprint == fingerprint && now - lastAttemptAt < 90_000L) {
                prefs.edit().putString(KEY_LAST_DECISION, "$summary\nMATCHED. Duplicate screen suppressed to avoid repeated taps.").apply()
                return
            }

            if (clickAcceptButton(root, 0)) {
                lastAttemptFingerprint = fingerprint
                lastAttemptAt = now
                prefs.edit()
                    .putString(KEY_LAST_ACTION, "Accept click sent; platform confirmation not verified")
                    .putString(KEY_LAST_DECISION, "$summary\nMATCHED. An Accept click action was sent. Verify the app confirms the ride; this app cannot confirm acceptance from the click alone.")
                    .apply()
            } else {
                prefs.edit()
                    .putString(KEY_LAST_ACTION, "No click: recognizable Accept control not found")
                    .putString(KEY_LAST_DECISION, "$summary\nMATCHED, but no enabled, clearly labelled Accept control was found. No action was sent.")
                    .apply()
            }
        } catch (e: Exception) {
            prefs.edit().putString(KEY_LAST_ACTION, "Monitor error: ${e.javaClass.simpleName}").apply()
        } finally {
            root.recycle()
        }
    }

    private fun loadRules(prefs: SharedPreferences) = RideRules(
        minFare = prefs.getFloat(KEY_MIN_FARE, 60f).toDouble(),
        maxPickupKm = prefs.getFloat(KEY_MAX_PICKUP, 2f).toDouble(),
        minTripKm = prefs.getFloat(KEY_MIN_TRIP, 5f).toDouble(),
        enabledPlatforms = splitSet(prefs.getString(KEY_PLATFORMS, "Rapido|Uber|Porter")),
        enabledVehicleTypes = splitSet(prefs.getString(KEY_VEHICLE_TYPES, "Bike|Auto|Cab|Parcel")),
        destinationKeyword = prefs.getString(KEY_DESTINATION, "") ?: "",
        filtersEnabled = prefs.getBoolean(KEY_FILTERS_ENABLED, true)
    )

    private fun splitSet(value: String?): Set<String> =
        value.orEmpty().split("|").map { it.trim() }.filter { it.isNotBlank() }.toSet()

    private fun parsePackageMap(value: String): Map<String, String> =
        value.lines().mapNotNull { line ->
            val parts = line.trim().split("=", limit = 2)
            if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) null
            else parts[0].trim() to parts[1].trim()
        }.toMap()

    private fun collectVisibleText(node: AccessibilityNodeInfo?, out: MutableList<String>, depth: Int) {
        if (node == null || depth > 24 || out.size >= 240) return
        node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() && it.length <= 300 }?.let(out::add)
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() && it.length <= 300 }?.let(out::add)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try { collectVisibleText(child, out, depth + 1) } finally { child.recycle() }
        }
    }

    private fun clickAcceptButton(node: AccessibilityNodeInfo?, depth: Int): Boolean {
        if (node == null || depth > 24) return false
        val labels = listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
            .map { it.trim().lowercase(Locale.ROOT).replace(Regex("""\s+"""), " ") }
        if (labels.any { it in ACCEPT_LABELS || ACCEPT_WITH_VALUE.matches(it) }) {
            if (clickNodeOrClickableAncestor(node)) return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val clicked = try { clickAcceptButton(child, depth + 1) } finally { child.recycle() }
            if (clicked) return true
        }
        return false
    }

    private fun clickNodeOrClickableAncestor(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth <= 4) {
            val isOriginal = current === node
            if (current.isEnabled && current.isClickable) {
                val success = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (success) {
                    if (!isOriginal) current.recycle()
                    return true
                }
            }
            val parent = current.parent
            if (!isOriginal) current.recycle()
            current = parent
            depth++
        }
        if (current != null && current !== node) current.recycle()
        return false
    }

    override fun onInterrupt() = Unit
}
