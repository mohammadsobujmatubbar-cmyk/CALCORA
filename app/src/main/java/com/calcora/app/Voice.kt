package com.calcora.app

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast

object VoiceParser {
    private const val BN_DIGITS = "০১২৩৪৫৬৭৮৯"

    private val words = mapOf(
        "শূন্য" to "0", "জিরো" to "0", "zero" to "0",
        "এক" to "1", "one" to "1", "দুই" to "2", "two" to "2",
        "তিন" to "3", "three" to "3", "চার" to "4", "four" to "4",
        "পাঁচ" to "5", "five" to "5", "ছয়" to "6", "six" to "6",
        "সাত" to "7", "seven" to "7", "আট" to "8", "eight" to "8",
        "নয়" to "9", "nine" to "9", "দশ" to "10", "ten" to "10"
    )

    private val ops = mapOf(
        "+" to "+", "যোগ" to "+", "plus" to "+", "add" to "+",
        "-" to "−", "−" to "−", "বিয়োগ" to "−", "minus" to "−", "subtract" to "−",
        "*" to "×", "×" to "×", "x" to "×", "গুণ" to "×", "গুন" to "×",
        "times" to "×", "multiply" to "×", "multiplied" to "×", "into" to "×",
        "/" to "÷", "÷" to "÷", "ভাগ" to "÷", "divided" to "÷", "divide" to "÷", "over" to "÷",
        "%" to "%", "শতাংশ" to "%", "percent" to "%",
        "দশমিক" to ".", "point" to ".", "dot" to "."
    )

    fun toExpr(raw: String): String? {
        var t = raw.lowercase()
        for ((i, c) in BN_DIGITS.withIndex()) t = t.replace(c, '0' + i)
        t = t.replace(Regex("(?<=\\d),(?=\\d{3})"), "")
        t = t.replace(Regex("(?<=\\d)\\s*x\\s*(?=\\d)"), " × ")
        t = t.replace(Regex("([+\\-−*/×÷%])"), " \$1 ")
        val sb = StringBuilder()
        for (tok in t.split(Regex("\\s+"))) {
            if (tok.isEmpty()) continue
            val m = ops[tok] ?: words[tok] ?: if (Regex("[0-9.]+").matches(tok)) tok else null
            if (m != null) sb.append(m)
        }
        val e = sb.toString()
        return if (e.any { it in "+−×÷" } && e.any { it.isDigit() }) e else null
    }

    fun toScreen(raw: String): Screen? {
        val t = raw.lowercase()
        fun has(vararg k: String) = k.any { t.contains(it) }
        return when {
            has("ক্যালকুলেটর", "calculator") -> Screen.Calculator
            has("হিস্টরি", "ইতিহাস", "history") -> Screen.History
            has("হাতের", "হাতে", "handwriting") -> Screen.Handwriting
            has("খাতা", "নোট", "notebook", "note") -> Screen.Notebooks
            has("সেটিং", "setting", "more") -> Screen.More
            has("হোম", "home") -> Screen.Home
            else -> null
        }
    }
}

class VoiceController(private val context: Context) {
    private var rec: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var active = false
    private var endAt = 0L
    private var lang = "bn-BD"
    private var onText: (String) -> Unit = {}
    private var onStop: () -> Unit = {}

    private val listener = object : RecognitionListener {
        override fun onResults(results: android.os.Bundle?) {
            val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = list?.firstOrNull()
            if (!text.isNullOrBlank()) onText(text)
            handler.postDelayed({ listen() }, 300)
        }

        override fun onError(error: Int) {
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                    handler.postDelayed({ listen() }, 300)
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                    handler.postDelayed({ listen() }, 800)
                else -> {
                    Toast.makeText(context, "Voice error: $error", Toast.LENGTH_SHORT).show()
                    stop()
                }
            }
        }

        override fun onReadyForSpeech(params: android.os.Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: android.os.Bundle?) {}
        override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
    }

    fun start(langCode: String, seconds: Int, onText: (String) -> Unit, onStop: () -> Unit) {
        this.onText = onText
        this.onStop = onStop
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "এই ফোনে Speech Recognition নেই", Toast.LENGTH_LONG).show()
            onStop()
            return
        }
        lang = if (langCode == "bn") "bn-BD" else "en-US"
        active = true
        endAt = SystemClock.elapsedRealtime() + seconds * 1000L
        rec = SpeechRecognizer.createSpeechRecognizer(context)
        rec?.setRecognitionListener(listener)
        listen()
    }

    private fun listen() {
        if (!active) return
        if (SystemClock.elapsedRealtime() >= endAt) {
            stop()
            return
        }
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        rec?.startListening(i)
    }

    fun stop() {
        active = false
        handler.removeCallbacksAndMessages(null)
        rec?.destroy()
        rec = null
        onStop()
    }
}
