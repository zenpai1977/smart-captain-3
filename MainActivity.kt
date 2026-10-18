package com.smartcaptain.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SmartCaptainScreen() }
    }
}

private data class CloudSyncResult(
    val active: Boolean,
    val message: String,
    val minFare: Double = 60.0,
    val maxPickupKm: Double = 2.0,
    val minTripKm: Double = 5.0,
    val destination: String = "",
    val filtersEnabled: Boolean = true
)

@Composable
private fun SmartCaptainScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(RideAccessibilityService.PREFS, Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var minFare by remember { mutableStateOf(prefs.getFloat(RideAccessibilityService.KEY_MIN_FARE, 60f).toString().removeSuffix(".0")) }
    var maxPickup by remember { mutableStateOf(prefs.getFloat(RideAccessibilityService.KEY_MAX_PICKUP, 2f).toString().removeSuffix(".0")) }
    var minTrip by remember { mutableStateOf(prefs.getFloat(RideAccessibilityService.KEY_MIN_TRIP, 5f).toString().removeSuffix(".0")) }
    var destination by remember { mutableStateOf(prefs.getString(RideAccessibilityService.KEY_DESTINATION, "") ?: "") }
    var phone by remember { mutableStateOf(prefs.getString(RideAccessibilityService.KEY_PHONE, "") ?: "") }
    var filtersEnabled by remember { mutableStateOf(prefs.getBoolean(RideAccessibilityService.KEY_FILTERS_ENABLED, true)) }
    var consent by remember { mutableStateOf(prefs.getBoolean(RideAccessibilityService.KEY_CONSENT, false)) }
    var autoAccept by remember { mutableStateOf(prefs.getBoolean(RideAccessibilityService.KEY_AUTO_ACCEPT, false)) }
    var passActive by remember { mutableStateOf(prefs.getBoolean(RideAccessibilityService.KEY_PASS_ACTIVE, false)) }
    var platforms by remember { mutableStateOf(splitPrefs(prefs.getString(RideAccessibilityService.KEY_PLATFORMS, "Rapido|Uber|Porter"))) }
    var vehicleTypes by remember { mutableStateOf(splitPrefs(prefs.getString(RideAccessibilityService.KEY_VEHICLE_TYPES, "Bike|Auto|Cab|Parcel"))) }
    var packageMap by remember { mutableStateOf(prefs.getString(RideAccessibilityService.KEY_PACKAGE_MAP, RideAccessibilityService.DEFAULT_PACKAGE_MAP) ?: RideAccessibilityService.DEFAULT_PACKAGE_MAP) }
    var message by remember { mutableStateOf("Save rules, sync your pass, then enable Accessibility.") }
    var lastDecision by remember { mutableStateOf(prefs.getString(RideAccessibilityService.KEY_LAST_DECISION, "No offer evaluated yet.") ?: "No offer evaluated yet.") }
    var lastText by remember { mutableStateOf(prefs.getString(RideAccessibilityService.KEY_LAST_TEXT, "No accessible text captured yet.") ?: "No accessible text captured yet.") }
    var lastPackage by remember { mutableStateOf(prefs.getString(RideAccessibilityService.KEY_LAST_PACKAGE, "None") ?: "None") }
    var serviceEnabled by remember { mutableStateOf(isServiceEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                serviceEnabled = isServiceEnabled(context)
                lastDecision = prefs.getString(RideAccessibilityService.KEY_LAST_DECISION, lastDecision) ?: lastDecision
                lastText = prefs.getString(RideAccessibilityService.KEY_LAST_TEXT, lastText) ?: lastText
                lastPackage = prefs.getString(RideAccessibilityService.KEY_LAST_PACKAGE, lastPackage) ?: lastPackage
                passActive = prefs.getBoolean(RideAccessibilityService.KEY_PASS_ACTIVE, passActive)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun saveAllRules() {
        val fare = minFare.toDoubleOrNull()
        val pickup = maxPickup.toDoubleOrNull()
        val trip = minTrip.toDoubleOrNull()
        if (fare == null || pickup == null || trip == null || fare < 0 || pickup < 0 || trip < 0) {
            message = "Enter valid, non-negative values for fare and distance."
            return
        }
        val cleanMap = packageMap.lines().filter {
            val parts = it.trim().split("=", limit = 2)
            parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()
        }.joinToString("\n") { it.trim() }
        prefs.edit()
            .putFloat(RideAccessibilityService.KEY_MIN_FARE, fare.toFloat())
            .putFloat(RideAccessibilityService.KEY_MAX_PICKUP, pickup.toFloat())
            .putFloat(RideAccessibilityService.KEY_MIN_TRIP, trip.toFloat())
            .putString(RideAccessibilityService.KEY_DESTINATION, destination.trim())
            .putString(RideAccessibilityService.KEY_PHONE, phone.trim())
            .putBoolean(RideAccessibilityService.KEY_FILTERS_ENABLED, filtersEnabled)
            .putBoolean(RideAccessibilityService.KEY_CONSENT, consent)
            .putBoolean(RideAccessibilityService.KEY_AUTO_ACCEPT, autoAccept && consent && passActive && filtersEnabled)
            .putString(RideAccessibilityService.KEY_PLATFORMS, platforms.joinToString("|"))
            .putString(RideAccessibilityService.KEY_VEHICLE_TYPES, vehicleTypes.joinToString("|"))
            .putString(RideAccessibilityService.KEY_PACKAGE_MAP, cleanMap)
            .apply()
        autoAccept = autoAccept && consent && passActive && filtersEnabled
        packageMap = cleanMap
        message = "Settings saved on this phone. Auto-accept is ${if (autoAccept) "ON" else "OFF"}."
    }

    MaterialTheme {
        Scaffold { padding ->
            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Smart Captain", style = MaterialTheme.typography.headlineMedium)
                Text("Apni pasand ki ride. Apne rules par.")
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("1. Ride filters", style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Enable filters")
                            Switch(checked = filtersEnabled, onCheckedChange = {
                                filtersEnabled = it
                                if (!it) autoAccept = false
                            })
                        }
                        OutlinedTextField(minFare, { minFare = it }, label = { Text("Minimum fare (₹)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(maxPickup, { maxPickup = it }, label = { Text("Maximum pickup (km)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(minTrip, { minTrip = it }, label = { Text("Minimum trip (km)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(destination, { destination = it }, label = { Text("Destination must contain (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                }
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("2. Allowed platforms", style = MaterialTheme.typography.titleMedium)
                        listOf("Rapido", "Uber", "Porter").forEach { p ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(p in platforms, { checked -> platforms = if (checked) platforms + p else platforms - p })
                                Text(p)
                            }
                        }
                        Divider()
                        Text("Vehicle / order types", style = MaterialTheme.typography.titleMedium)
                        listOf("Bike", "Auto", "Cab", "Parcel").forEach { type ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(type in vehicleTypes, { checked -> vehicleTypes = if (checked) vehicleTypes + type else vehicleTypes - type })
                                Text(type)
                            }
                        }
                    }
                }
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("3. Sync existing cloud pass", style = MaterialTheme.typography.titleMedium)
                        Text("Enter the same 10-digit phone number saved on the Smart Captain website. Sync reads the existing Render/Supabase driver status and saved rules.")
                        OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(10) }, label = { Text("Driver phone number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Button(onClick = {
                            scope.launch {
                                message = "Checking cloud pass…"
                                val result = withContext(Dispatchers.IO) { syncCloudPass(phone) }
                                passActive = result.active
                                message = result.message
                                if (result.active) {
                                    minFare = result.minFare.toString()
                                    maxPickup = result.maxPickupKm.toString()
                                    minTrip = result.minTripKm.toString()
                                    destination = result.destination
                                    filtersEnabled = result.filtersEnabled
                                    prefs.edit()
                                        .putBoolean(RideAccessibilityService.KEY_PASS_ACTIVE, true)
                                        .putFloat(RideAccessibilityService.KEY_MIN_FARE, result.minFare.toFloat())
                                        .putFloat(RideAccessibilityService.KEY_MAX_PICKUP, result.maxPickupKm.toFloat())
                                        .putFloat(RideAccessibilityService.KEY_MIN_TRIP, result.minTripKm.toFloat())
                                        .putString(RideAccessibilityService.KEY_DESTINATION, result.destination)
                                        .putBoolean(RideAccessibilityService.KEY_FILTERS_ENABLED, result.filtersEnabled)
                                        .putString(RideAccessibilityService.KEY_PHONE, phone)
                                        .apply()
                                } else {
                                    prefs.edit().putBoolean(RideAccessibilityService.KEY_PASS_ACTIVE, false)
                                        .putBoolean(RideAccessibilityService.KEY_AUTO_ACCEPT, false).apply()
                                    autoAccept = false
                                }
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Sync rules & check pass") }
                        Text(if (passActive) "Cloud pass: ACTIVE" else "Cloud pass: not confirmed active")
                        Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://smart-captain.onrender.com"))) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Open Smart Captain website")
                        }
                    }
                }
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("4. Accessibility & auto-accept", style = MaterialTheme.typography.titleMedium)
                        Text("Accessibility reads visible text in the exact package IDs below. The latest sample is stored locally to diagnose filters; this app does not upload screen text.")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(consent, { checked ->
                                consent = checked
                                if (!checked) autoAccept = false
                                prefs.edit().putBoolean(RideAccessibilityService.KEY_CONSENT, checked)
                                    .putBoolean(RideAccessibilityService.KEY_AUTO_ACCEPT, autoAccept && checked).apply()
                                if (!checked) {
                                    prefs.edit().remove(RideAccessibilityService.KEY_LAST_TEXT)
                                        .putString(RideAccessibilityService.KEY_LAST_DECISION, "Consent is off; monitoring stopped.")
                                        .apply()
                                    lastText = "Consent is off."
                                    lastDecision = "Consent is off; monitoring stopped."
                                }
                            })
                            Text("I consent to local screen-text monitoring.")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Auto-accept matching offers")
                                Text("Requires active cloud pass, consent, and filters ON. Start OFF.", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(autoAccept, { checked ->
                                if (checked && (!consent || !passActive || !filtersEnabled)) {
                                    autoAccept = false
                                    message = "To enable auto-accept, sync an active pass, enable filters, give consent, and save settings."
                                } else autoAccept = checked
                            })
                        }
                        Text("Accessibility service: ${if (serviceEnabled) "ENABLED" else "NOT ENABLED"}")
                        Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Open Android Accessibility settings")
                        }
                        Button(onClick = {
                            lastDecision = prefs.getString(RideAccessibilityService.KEY_LAST_DECISION, "No offer evaluated yet.") ?: "No offer evaluated yet."
                            lastText = prefs.getString(RideAccessibilityService.KEY_LAST_TEXT, "No text captured yet.") ?: "No text captured yet."
                            lastPackage = prefs.getString(RideAccessibilityService.KEY_LAST_PACKAGE, "None") ?: "None"
                            serviceEnabled = isServiceEnabled(context)
                            passActive = prefs.getBoolean(RideAccessibilityService.KEY_PASS_ACTIVE, false)
                        }, modifier = Modifier.fillMaxWidth()) { Text("Refresh monitor result") }
                        Text("Last app: $lastPackage", style = MaterialTheme.typography.bodySmall)
                        Text(lastDecision, style = MaterialTheme.typography.bodyMedium)
                        Text("Latest screen text (local only):", style = MaterialTheme.typography.labelMedium)
                        Text(lastText, style = MaterialTheme.typography.bodySmall)
                        Text("A click attempt is not confirmation that a ride was accepted. Check the driver app.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("5. Driver-app package IDs", style = MaterialTheme.typography.titleMedium)
                        Text("One entry per line: package.id=Platform. Allowed names: Rapido, Uber, Porter. Verify IDs on your phone; defaults are not guaranteed for every version or region.")
                        OutlinedTextField(packageMap, { packageMap = it }, label = { Text("Package ID mappings") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    }
                }
                Button(onClick = { saveAllRules() }, modifier = Modifier.fillMaxWidth()) { Text("Save all settings") }
                Text(message)
                Text("Test while parked only. Driver-app interfaces can change, and use of accessibility automation may be restricted by platform terms. Never configure or rely on the phone while the vehicle is moving.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun splitPrefs(value: String?): Set<String> =
    value.orEmpty().split("|").map { it.trim() }.filter { it.isNotBlank() }.toSet()

private fun isServiceEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { it.resolveInfo.serviceInfo.packageName == context.packageName &&
            it.resolveInfo.serviceInfo.name == RideAccessibilityService::class.java.name }
}

private fun syncCloudPass(phone: String): CloudSyncResult {
    if (!phone.matches(Regex("""^\d{10}$"""))) {
        return CloudSyncResult(false, "Enter the same valid 10-digit phone number used on the website.")
    }
    var connection: HttpURLConnection? = null
    return try {
        val encoded = URLEncoder.encode(phone, Charsets.UTF_8.name())
        connection = URL("https://smart-captain.onrender.com/api/driver?phone=$encoded")
            .openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 12_000
        connection.readTimeout = 12_000
        connection.setRequestProperty("Accept", "application/json")
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        val json = JSONObject(body)
        if (!json.optBoolean("active", false)) {
            return CloudSyncResult(false, json.optString("message", "Cloud pass is not active."))
        }
        val rules = json.optJSONObject("rules") ?: JSONObject()
        CloudSyncResult(
            active = true,
            message = "Cloud pass active; rules synced from the existing Smart Captain backend.",
            minFare = rules.optDouble("minFare", 60.0).coerceAtLeast(0.0),
            maxPickupKm = rules.optDouble("maxPickupKm", 2.0).coerceAtLeast(0.0),
            minTripKm = rules.optDouble("minTripKm", 5.0).coerceAtLeast(0.0),
            destination = rules.optString("goToArea", ""),
            filtersEnabled = rules.optBoolean("activeSwitch", true)
        )
    } catch (e: Exception) {
        CloudSyncResult(false, "Cloud sync failed: ${e.message ?: e.javaClass.simpleName}")
    } finally {
        connection?.disconnect()
    }
}
