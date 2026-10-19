package com.smartcaptain.app

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Outer scroll container so all content is reachable on mobile screens
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0D1117"))
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 48, 40, 64)
        }
        scrollView.addView(root)

        // App Title Header
        val title = TextView(this).apply {
            text = "Smart Captain"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }
        val subtitle = TextView(this).apply {
            text = "Driver Rule Engine & Dashboard"
            textSize = 13f
            setTextColor(Color.parseColor("#8B949E"))
            setPadding(0, 4, 0, 24)
        }
        root.addView(title)
        root.addView(subtitle)

        // Status Display Banner
        val statusBanner = TextView(this).apply {
            text = "ENGINE STATUS: READY"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#3FB950"))
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.parseColor("#161B22"))
        }
        root.addView(statusBanner)

        // Master Switch Control
        val masterSwitch = Switch(this).apply {
            text = "Master Enable Filter Engine"
            textSize = 16f
            setTextColor(Color.WHITE)
            isChecked = true
            setPadding(0, 24, 0, 16)
        }
        root.addView(masterSwitch)

        // Section 1: Driver Phone Input
        root.addView(sectionHeader("1. Driver Account Settings"))
        val phoneInput = makeInput("10-Digit Mobile Number", "", InputType.TYPE_CLASS_PHONE)
        root.addView(phoneInput)

        // Section 2: Rule Configuration Inputs
        root.addView(sectionHeader("2. Ride Filtering Parameters"))
        
        root.addView(label("Minimum Trip Distance (km):"))
        val minTripInput = makeInput("5.0", "5.0", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        root.addView(minTripInput)

        root.addView(label("Max Pickup Distance (km):"))
        val maxPickupInput = makeInput("2.0", "2.0", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        root.addView(maxPickupInput)

        root.addView(label("Minimum Fare (₹):"))
        val minFareInput = makeInput("60", "60", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        root.addView(minFareInput)

        root.addView(label("Preferred Destination / Go-To Area:"))
        val goToInput = makeInput("e.g. Connaught Place", "", InputType.TYPE_CLASS_TEXT)
        root.addView(goToInput)

        // Save Button
        val saveBtn = Button(this).apply {
            text = "Save Configuration"
            setBackgroundColor(Color.parseColor("#238636"))
            setTextColor(Color.WHITE)
            setPadding(0, 20, 0, 20)
        }
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 24, 0, 24)
        saveBtn.layoutParams = params
        root.addView(saveBtn)

        // Section 3: Simulator Output Panel
        root.addView(sectionHeader("3. Rule Simulator Output"))
        val outputView = TextView(this).apply {
            text = "Engine is initialized. Adjust rules above to evaluate incoming rides."
            setTextColor(Color.WHITE)
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.parseColor("#161B22"))
        }
        root.addView(outputView)

        setContentView(scrollView)
    }

    private fun sectionHeader(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, 32, 0, 12)
        }
    }

    private fun label(textStr: String): TextView {
        return TextView(this).apply {
            text = textStr
            textSize: Float = 13f
            setTextColor(Color.parseColor("#8B949E"))
            setPadding(0, 8, 0, 4)
        }
    }

    private fun makeInput(hintStr: String, initial: String, inputTypeFlag: Int): EditText {
        return EditText(this).apply {
            hint = hintStr
            setText(initial)
            inputType = inputTypeFlag
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#484F58"))
            setBackgroundColor(Color.parseColor("#161B22"))
            setPadding(24, 20, 24, 20)
        }
    }
}
