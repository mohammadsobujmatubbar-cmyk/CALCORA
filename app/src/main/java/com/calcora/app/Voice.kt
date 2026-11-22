package com.calcora.app

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import java.util.Locale

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

    private fun any(raw: String, vararg k: String): Boolean {
        val t = raw.lowercase()
        return k.any { t.contains(it) }
    }

    fun wantsCalc(raw: String) = any(raw, "ক্যালকুলেটর", "calculator", "হিসাব")
    fun wantsNote(raw: String) = any(raw, "নোট", "খাতা", "note")
    fun switchToNote(raw: String) = any(raw, "নোট খাতা", "নোট মোড", "note mode", "notebook")
    fun switchToCalc(raw: String) = any(raw, "ক্যালকুলেটর মোড", "calculator mode")
    fun wantsStop(raw: String) = raw.trim().lowercase() in setOf("বন্ধ", "বন্ধ করো", "থামো", "stop")

    fun spoken(s: String, bn: Boolean): String {
        val sb = StringBuilder()
        for (c in s) {
            val w = when (c) {
                '+' -> if (bn) " যোগ " else " plus "
                '−', '-' -> if (bn) " বিয়োগ " else " minus "
                '×' -> if (bn) " গুণ " else " times "
                '÷' -> if (bn) " ভাগ " else " divided by "
                '%' -> if (bn) " শতাংশ " else " percent "
                '.' -> if (bn) " দশমিক " else " point "
                in '0'..'9' -> if (bn) BN_DIGITS[c - '0'].toString() else c.toString()
                else -> c.toString()
            }
            sb.append(w)
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }
}

class VoiceController(private val context: Context) {
    private var rec: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var active = false
    private var endAt = 0L
    private var seconds = 30
    private var lang = "bn-BD"
    private var onText: (String) -> Unit = {}
    private var onStop: () -> Unit = {}

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speaking = false
    private var speakStart = 0L
    var speaksBangla = false
        private set

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) { finishSpeak() }
                    override fun onError(utteranceId: String?) { finishSpeak() }
                })
            }
        }
    }

    private fun applyTtsLang(wantBn: Boolean) {
        val t = tts ?: return
        speaksBangla = false
        if (wantBn) {
            for (loc in listOf(Locale("bn", "BD"), Locale("bn", "IN"))) {
                if (t.setLanguage(loc) >= TextToSpeech.LANG_AVAILABLE) {
                    speaksBangla = true
                    return
                }
            }
        }
        t.setLanguage(Locale.US)
    }

    fun say(bn: String, en: String) {
        val t = tts
        if (!ttsReady || t == null) return
        speaking = true
        speakStart = SystemClock.elapsedRealtime()
        t.speak(if (speaksBangla) bn else en, TextToSpeech.QUEUE_FLUSH, null, "calcora")
    }

    private fun finishSpeak() {
        handler.postDelayed({
            speaking = false
            touch()
        }, 350)
    }

    private fun touch() {
        endAt = SystemClock.elapsedRealtime() + seconds * 1000L
    }

    private val listener = object : RecognitionListener {
        override fun onResults(results: android.os.Bundle?) {
            val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = list?.firstOrNull()
            touch()
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

    fun start(
        langCode: String,
        secs: Int,
        introBn: String,
        introEn: String,
        onText: (String) -> Unit,
        onStop: () -> Unit
    ) {
        this.onText = onText
        this.onStop = onStop
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "এই ফোনে Speech Recognition নেই", Toast.LENGTH_LONG).show()
            onStop()
            return
        }
        seconds = secs
        lang = if (langCode == "bn") "bn-BD" else "en-US"
        applyTtsLang(langCode == "bn")
        active = true
        touch()
        rec = SpeechRecognizer.createSpeechRecognizer(context)
        rec?.setRecognitionListener(listener)
        say(introBn, introEn)
        listen()
    }

    private fun listen() {
        if (!active) return
        if (speaking) {
            if (SystemClock.elapsedRealtime() - speakStart > 15000) speaking = false
            handler.postDelayed({ listen() }, 300)
            return
        }
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
        speaking = false
        handler.removeCallbacksAndMessages(null)
        tts?.stop()
        rec?.destroy()
        rec = null
        onStop()
    }

    fun release() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
