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

        val scrollView = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#0D1117"))
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(40, 60, 40, 80)
        }
        scrollView.addView(root)

        val title = TextView(this).apply {
            text = "SMART CAPTAIN"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#58A6FF"))
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Rule Engine & Dashboard Active"
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(0, 8, 0, 32)
        }
        root.addView(subtitle)

        val statusBanner = TextView(this).apply {
            text = "STATUS: ENGINE READY"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.parseColor("#238636"))
        }
        root.addView(statusBanner)

        val label = TextView(this).apply {
            text = "Minimum Fare (₹):"
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(0, 32, 0, 8)
        }
        root.addView(label)

        val minFareInput = EditText(this).apply {
            setText("60")
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setBackgroundColor(Color.parseColor("#21262D"))
            setPadding(24, 24, 24, 24)
        }
        root.addView(minFareInput)

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

        setContentView(scrollView)
    }
}
