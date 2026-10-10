package com.skkaushal.voiceassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var response: TextView
    private lateinit var apiKeyInput: EditText
    private lateinit var tts: TextToSpeech
    private var recognizer: SpeechRecognizer? = null
    private var wakeMode = false
    private var ttsReady = false
    private val worker = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(8, 12, 28)
        window.navigationBarColor = Color.rgb(8, 12, 28)
        tts = TextToSpeech(this, this)
        buildUi()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 71)
        }
    }

    private fun buildUi() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(Color.rgb(8, 12, 28))
        }
        fun label(text: String, size: Float, color: Int = Color.WHITE) = TextView(this).apply {
            this.text = text; textSize = size; setTextColor(color); gravity = Gravity.CENTER
        }
        page.addView(label("SK KAUSHAL", 15f, Color.rgb(104, 192, 255)))
        page.addView(label("SK KAUSHAL VOICE ASSISTANT", 23f).apply {
            setPadding(0, 6, 0, 4); setTypeface(null, android.graphics.Typeface.BOLD)
        })
        status = label("Ready • Hindi + English", 14f, Color.LTGRAY)
        page.addView(status)
        page.addView(label("◉", 88f, Color.rgb(90, 160, 255)).apply { setPadding(0, 12, 0, 12) })
        val mic = Button(this).apply {
            text = "🎙  Speak to SK"
            setOnClickListener { startListening(false) }
        }
        page.addView(mic, matchWrap())
        val wake = Button(this).apply {
            text = "Enable “Hey SK” (while app is open)"
            setOnClickListener {
                wakeMode = !wakeMode
                text = if (wakeMode) "Disable “Hey SK”" else "Enable “Hey SK” (while app is open)"
                if (wakeMode) startListening(true) else {
                    recognizer?.cancel()
                    status.text = "Wake phrase disabled"
                }
            }
        }
        page.addView(wake, matchWrap())
        transcript = label("You: —", 16f).apply {
            gravity = Gravity.START; setPadding(0, 18, 0, 8)
        }
        page.addView(transcript, matchWrap())
        response = label("SK: Namaste! Main aapki kya help karun?", 17f).apply {
            gravity = Gravity.START; setPadding(0, 8, 0, 12)
        }
        val scroll = ScrollView(this).apply { addView(response) }
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        apiKeyInput = EditText(this).apply {
            hint = "Gemini API key (optional; kept only in this session)"
            setHintTextColor(Color.GRAY); setTextColor(Color.WHITE)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
        }
        page.addView(apiKeyInput, matchWrap())
        page.addView(Button(this).apply {
            text = "Open Android Settings"
            setOnClickListener { startActivity(Intent(Settings.ACTION_SETTINGS)) }
        }, matchWrap())
        setContentView(page)
    }

    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 8 }

    private fun startListening(forWake: Boolean) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 71)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "Speech recognition unavailable on this device"
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { status.text = if (forWake) "Listening for Hey SK…" else "Listening…" }
            override fun onBeginningOfSpeech() { status.text = "Hearing you…" }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { status.text = "Thinking…" }
            override fun onError(error: Int) {
                status.text = "Speech ended (code $error)"
                if (forWake && wakeMode && !isFinishing) {
                    window.decorView.postDelayed({ if (wakeMode) startListening(true) }, 900)
                }
            }
            override fun onResults(results: Bundle?) {
                val said = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty()
                if (forWake) {
                    if (said.contains("hey sk", true) || said.contains("hey es kay", true) ||
                        said.contains("हे एसके", true)) {
                        val command = said.replace(Regex("(?i)hey\\s*s\\s*k|hey\\s*es\\s*kay|हे\\s*एसके"), "").trim()
                        if (command.isNotBlank()) handleCommand(command) else {
                            speak("Yes, boliye. Main sun raha hoon.")
                            startListening(false)
                        }
                    }
                    if (wakeMode && !isFinishing) {
                        window.decorView.postDelayed({ if (wakeMode) startListening(true) }, 700)
                    }
                } else if (said.isNotBlank()) {
                    transcript.text = "You: $said"
                    handleCommand(said)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try { recognizer?.startListening(intent) } catch (e: Exception) {
            status.text = "Could not start microphone: ${e.localizedMessage}"
        }
    }

    private fun handleCommand(command: String) {
        transcript.text = "You: $command"
        val normalized = command.lowercase(Locale.ROOT)
        val local = when {
            normalized.contains("open settings") || normalized.contains("सेटिंग") -> {
                startActivity(Intent(Settings.ACTION_SETTINGS)); "Opening Android Settings."
            }
            normalized.contains("open wifi") || normalized.contains("wi-fi") || normalized.contains("वाईफाई") -> {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)); "Opening Wi-Fi settings."
            }
            normalized.contains("open bluetooth") || normalized.contains("ब्लूटूथ") -> {
                startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); "Opening Bluetooth settings."
            }
            normalized.contains("open camera") || normalized.contains("कैमरा खोल") -> {
                try { startActivity(packageManager.getLaunchIntentForPackage("com.android.camera") ?: Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)); "Opening camera." }
                catch (_: Exception) { "Camera app could not be opened." }
            }
            normalized.contains("what time") || normalized.contains("time now") || normalized.contains("समय") ->
                "The current device time is ${java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()).format(java.util.Date())}."
            normalized.contains("hello") || normalized.contains("नमस्ते") || normalized.contains("hi sk") ->
                "Namaste! Main SK Kaushal Voice Assistant hoon. Bataiye, kya help chahiye?"
            else -> null
        }
        if (local != null) { showAnswer(local); return }
        val key = apiKeyInput.text.toString().trim()
        if (key.isBlank()) {
            showAnswer("Is sawal ka AI jawab dene ke liye Gemini API key enter karein. Local commands jaise Settings, Wi-Fi aur Bluetooth bina key ke chal sakte hain.")
            return
        }
        status.text = "Gemini se jawab aa raha hai…"
        worker.execute {
            val result = GeminiManager.ask(key, command)
            runOnUiThread { showAnswer(result) }
        }
    }

    private fun showAnswer(text: String) {
        response.text = "Sk Kaushal: $text"
        status.text = "Ready"
        speak(text)
    }

    private fun speak(text: String) {
        if (ttsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sk-response")
    }

    override fun onInit(statusCode: Int) {
        ttsReady = statusCode == TextToSpeech.SUCCESS
        if (ttsReady) tts.language = Locale("en", "IN")
    }

    override fun onDestroy() {
        wakeMode = false
        recognizer?.destroy()
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        worker.shutdownNow()
        super.onDestroy()
    }
}
