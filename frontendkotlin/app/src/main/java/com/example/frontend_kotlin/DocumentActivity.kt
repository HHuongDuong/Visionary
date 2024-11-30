package com.example.frontend_kotlin

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DocumentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document)

        val responseText = intent.getStringExtra("responseText")
        val textView: TextView = findViewById(R.id.responseTextView)
        textView.text = responseText
    }

    fun onReturnClick(view: View) {
        MainActivity.tts.stop()
        finish()
    }
}