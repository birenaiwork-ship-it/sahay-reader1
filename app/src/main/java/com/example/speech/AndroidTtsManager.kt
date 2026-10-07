package com.example.speech

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class AndroidTtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isEnglishSupported = MutableStateFlow(true)
    val isEnglishSupported: StateFlow<Boolean> = _isEnglishSupported.asStateFlow()

    private val _isOdiaSupported = MutableStateFlow(false)
    val isOdiaSupported: StateFlow<Boolean> = _isOdiaSupported.asStateFlow()

    private var onDoneCallback: (() -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null
    private val synthesisCallbacks = ConcurrentHashMap<String, (Boolean) -> Unit>()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .build()
            )

            // Check English support
            val enStatus = tts?.isLanguageAvailable(Locale.US) ?: TextToSpeech.LANG_NOT_SUPPORTED
            _isEnglishSupported.value = enStatus >= TextToSpeech.LANG_AVAILABLE

            // Check Odia support
            val odiaLocale = Locale.forLanguageTag("or-IN")
            val odiaStatus = tts?.isLanguageAvailable(odiaLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
            _isOdiaSupported.value = odiaStatus >= TextToSpeech.LANG_AVAILABLE

            setupProgressListener()
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (utteranceId?.startsWith("utterance_") == true) {
                    _isSpeaking.value = true
                }
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId?.startsWith("synth_") == true) {
                    synthesisCallbacks.remove(utteranceId)?.invoke(true)
                } else {
                    _isSpeaking.value = false
                    onDoneCallback?.invoke()
                }
            }

            override fun onError(utteranceId: String?) {
                if (utteranceId?.startsWith("synth_") == true) {
                    synthesisCallbacks.remove(utteranceId)?.invoke(false)
                } else {
                    _isSpeaking.value = false
                    onErrorCallback?.invoke("TTS playback error for utterance $utteranceId")
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                if (utteranceId?.startsWith("synth_") == true) {
                    synthesisCallbacks.remove(utteranceId)?.invoke(false)
                } else {
                    _isSpeaking.value = false
                    onErrorCallback?.invoke("TTS playback error ($errorCode)")
                }
            }
        })
    }

    fun speak(
        text: String,
        language: String, // "en" or "or"
        speedRate: Float,
        onDone: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ): Boolean {
        if (!isInitialized || tts == null) {
            onError?.invoke("Speech engine is not initialized yet")
            return false
        }

        stop()
        this.onDoneCallback = onDone
        this.onErrorCallback = onError

        val locale = if (language.startsWith("or", ignoreCase = true)) {
            Locale.forLanguageTag("or-IN")
        } else {
            Locale.US
        }

        val langResult = tts?.setLanguage(locale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            if (language.startsWith("or", ignoreCase = true)) {
                onError?.invoke("Odia language voice is not installed in the device TTS engine. Please check TTS settings or configure cloud voice.")
            } else {
                onError?.invoke("English voice is not available in TTS engine.")
            }
            return false
        }

        tts?.setSpeechRate(speedRate.coerceIn(0.5f, 2.0f))
        val utteranceId = "utterance_${System.currentTimeMillis()}"
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        val speakResult = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        return speakResult == TextToSpeech.SUCCESS
    }

    fun synthesizeToFile(
        text: String,
        language: String,
        speedRate: Float,
        outputFile: File,
        onComplete: (Boolean) -> Unit
    ) {
        if (!isInitialized || tts == null) {
            onComplete(false)
            return
        }

        val locale = if (language.startsWith("or", ignoreCase = true)) {
            Locale.forLanguageTag("or-IN")
        } else {
            Locale.US
        }
        val langResult = tts?.setLanguage(locale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            onComplete(false)
            return
        }

        tts?.setSpeechRate(speedRate.coerceIn(0.5f, 2.0f))
        val utteranceId = "synth_${System.currentTimeMillis()}_${System.nanoTime()}"
        synthesisCallbacks[utteranceId] = onComplete

        val params = Bundle()
        tts?.synthesizeToFile(text, params, outputFile, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
