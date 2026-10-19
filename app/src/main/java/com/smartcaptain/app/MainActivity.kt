package com.smartcaptain.app

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. ScrollView must fill the entire screen (MATCH_PARENT)
        val scrollView = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#0D1117"))
        }

        // 2. Root layout must match parent width and wrap content vertically
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(40, 60, 40, 80)
        }
        scrollView.addView(root)

        // Title Header
        val title = TextView(this).apply {
            text = "SMART CAPTAIN"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Rule Engine Dashboard Active"
            textSize = 13f
            setTextColor(Color.parseColor("#58A6FF"))
            setPadding(0, 8, 0, 32)
        }
        root.addView(subtitle)

        // Status Banner
        val statusBanner = TextView(this).apply {
            text = "STATUS: READY"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.parseColor("#238636"))
        }
        root.addView(statusBanner)

        // Minimum Fare Field
        root.addView(label("Minimum Fare (₹):"))
        val minFareInput = EditText(this).apply {
            setText("60")
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#30363D"))
            setPadding(24, 24, 24, 24)
        }
        root.addView(minFareInput)

        // Save Button
        val saveBtn = Button(this).apply {
            text = "SAVE CONFIGURATION"
            setBackgroundColor(Color.parseColor("#1F6FEB"))
            setTextColor(Color.WHITE)
            setPadding(0, 24, 0, 24)
        }
        val btnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        btnParams.setMargins(0, 32, 0, 32)
        saveBtn.layoutParams = btnParams
        root.addView(saveBtn)

        // Set the scroll view as the main content view
        setContentView(scrollView)
    }

    private fun label(textStr: String): TextView {
        return TextView(this).apply {
            text = textStr
            textSize = 13f
            setTextColor(Color.parseColor("#C9D1D9"))
            setPadding(0, 24, 0, 8)
        }
    }
}
