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

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0D1117")) // Dark background matching website
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 40, 32, 64)
        }
        scrollView.addView(root)

        // --- TOP BANNER CARD ---
        val topCard = createCard(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        val topTitle = TextView(this).apply {
            text = "smart captain — Apni pasand ki ride. Apne rules par."
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }
        val topSub = TextView(this).apply {
            text = "Supabase Cloud DB Connected • Filter rides automatically."
            textSize = 12f
            setTextColor(Color.parseColor("#8B949E"))
            setPadding(0, 8, 0, 0)
        }
        topCard.addView(topTitle)
        topCard.addView(topSub)
        root.addView(topCard)

        // --- CARD 1: RIDE FILTER RULES ---
        val rulesCard = createCard(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        rulesCard.addView(sectionHeader("1. Aapke Ride Filter Rules"))

        val switchLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 16)
        }
        val switchLabel = TextView(this).apply {
            text = "Bike / Auto / Cab / Parcel Switch"
            setTextColor(Color.WHITE)
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val serviceSwitch = CheckBox(this).apply {
            isChecked = true
        }
        switchLayout.addView(switchLabel)
        switchLayout.addView(serviceSwitch)
        rulesCard.addView(switchLayout)

        val minTripInput = addInputField(rulesCard, "Minimum Trip Doori (km)", "5")
        val maxPickupInput = addInputField(rulesCard, "Max Pickup Doori (km)", "2")
        val minFareInput = addInputField(rulesCard, "Minimum Kamai (₹ Fare)", "60")
        val goToInput = addInputField(rulesCard, "Go To (Ghar / Ilaaka)", "e.g. Connaught Place")

        root.addView(rulesCard)

        // --- CARD 2: ACTIVATION & PASS ---
        val activationCard = createCard(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        activationCard.addView(sectionHeader("2. Direct GPay Pass & Rule Activation"))

        val phoneInput = addInputField(activationCard, "Driver Mobile Number (10 Digits):", "9656896787")

        activationCard.addView(label("Select Pass Plan:"))
        val planSpinner = Spinner(this).apply {
            val plans = arrayOf("1 Day Pass — ₹9 (Unlimited Rides)", "7 Days Pass — ₹49", "30 Days Pass — ₹149")
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, plans)
            setBackgroundColor(Color.parseColor("#21262D"))
            setPadding(16, 16, 16, 16)
        }
        activationCard.addView(planSpinner)

        val payBtn = Button(this).apply {
            text = "Tap to Pay ₹9 Directly on GPay / UPI App"
            setBackgroundColor(Color.parseColor("#1F6FEB"))
            setTextColor(Color.WHITE)
            setPadding(0, 24, 0, 24)
        }
        val payParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        payParams.setMargins(0, 24, 0, 24)
        payBtn.layoutParams = payParams
        activationCard.addView(payBtn)

        val saveRulesBtn = Button(this).apply {
            text = "Save Rules & Submit for Verification"
            setBackgroundColor(Color.parseColor("#238636"))
            setTextColor(Color.WHITE)
            setPadding(0, 24, 0, 24)
        }
        saveRulesBtn.layoutParams = payParams
        activationCard.addView(saveRulesBtn)

        val checkStatusBtn = Button(this).apply {
            text = "Check My Pass Status"
            setBackgroundColor(Color.parseColor("#21262D"))
            setTextColor(Color.parseColor("#58A6FF"))
            setPadding(0, 16, 0, 16)
        }
        activationCard.addView(checkStatusBtn)

        root.addView(activationCard)

        setContentView(scrollView)
    }

    private fun createCard(context: android.content.Context): LinearLayout {
        return LinearLayout(context).apply {
            setBackgroundColor(Color.parseColor("#161B22"))
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, 0, 24)
            layoutParams = params
        }
    }

    private fun sectionHeader(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 16)
        }
    }

    private fun label(textStr: String): TextView {
        return TextView(this).apply {
            text = textStr
            textSize = 13f
            setTextColor(Color.parseColor("#8B949E"))
            setPadding(0, 16, 0, 6)
        }
    }

    private fun addInputField(parent: LinearLayout, labelText: String, hintText: String): EditText {
        parent.addView(label(labelText))
        val editText = EditText(this).apply {
            hint = hintText
            setText(hintText)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#484F58"))
            setBackgroundColor(Color.parseColor("#21262D"))
            setPadding(20, 16, 20, 16)
            textSize = 14f
        }
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 0, 0, 12)
        editText.layoutParams = params
        parent.addView(editText)
        return editText
    }
}
