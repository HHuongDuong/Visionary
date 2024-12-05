package com.example.frontend_kotlin.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

object TTS : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var onSpeechFinished: (() -> Unit)? = null

    fun initialize(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context, this)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    // Do nothing
                }

                override fun onDone(utteranceId: String?) {
                    onSpeechFinished?.invoke()
                }

                override fun onError(utteranceId: String?) {
                    // Handle error
                }
            })
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("vi", "VN")
            isInitialized = true
        } else {
            Log.e("TextToSpeechSingleton", "TextToSpeech initialization failed")
        }
    }

    fun speak(text: String, queueFlush: Int, para1: Nothing?, para2: String?, onSpeechFinished: (() -> Unit)? = null) {
        if (isInitialized) {
            this.onSpeechFinished = onSpeechFinished
            tts?.speak(text, queueFlush, para1, para2)
        } else {
            Log.e("TextToSpeechSingleton", "TextToSpeech not initialized")
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}