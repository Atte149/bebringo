package org.dueattendant149.bebringo.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dueattendant149.bebringo.data.prefs.SettingsStore
import org.dueattendant149.bebringo.di.ServiceLocator
import org.json.JSONObject
import java.util.Locale

/**
 * Гибридный аудиодвижок Bebringo.
 *
 * 1. Первый приоритет: Запечённые студийные Ogg Opus файлы из assets/audio/ro_a1/
 *    (воспроизводятся через MediaPlayer мгновенно, без задержек и 100% офлайн).
 * 2. Второй приоритет: Системный TextToSpeech для динамических слов (режим чтения со словарём).
 */
class TtsPlayer(
    private val context: Context,
    private val settings: SettingsStore = ServiceLocator.settings,
) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var ttsReady = false
    private var romanianTtsAvailable = false
    private var mediaPlayer: MediaPlayer? = null
    private var soundEnabled = true

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // Карта нормализованных фраз: "bună dimineața" -> "bun_diminea_a_f8fa541fafc9.ogg"
    private val offlineManifest = HashMap<String, String>()

    private val tts = TextToSpeech(appContext, this)

    init {
        loadOfflineManifest()
        scope.launch {
            settings.soundEnabled.collect { enabled ->
                soundEnabled = enabled
                if (!enabled) stop()
            }
        }
    }

    private fun loadOfflineManifest() {
        runCatching {
            val stream = appContext.assets.open("audio/ro_a1/manifest.json")
            val jsonText = stream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonText)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                offlineManifest[key.lowercase().trim()] = json.getString(key)
            }
            Log.i(TAG, "Успешно загружен офлайн-манифест аудио: ${offlineManifest.size} фраз")
        }.onFailure { e ->
            Log.w(TAG, "Офлайн-манифест аудио не найден: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "Системный TTS не инициализировался: $status")
            return
        }
        val result = tts.setLanguage(ROMANIAN)
        romanianTtsAvailable = result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED
        if (!romanianTtsAvailable) {
            Log.w(TAG, "Румынский системный голос недоступен, используется системный fallback")
            tts.setLanguage(Locale.getDefault())
        }
        // Медиа-поток: системная громкость и беззвучный режим должны влиять на озвучку.
        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build(),
        )
        tts.setSpeechRate(0.92f)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isPlaying.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isPlaying.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isPlaying.value = false
            }
        })
        ttsReady = true
    }

    /** Проверяет, запечено ли офлайн-аудио для данного текста. */
    fun hasOfflineAudio(text: String): Boolean {
        val normalized = normalizeKey(text)
        val stripped = normalized.trimEnd('.', '!', '?', ',', ':', ';')
        return offlineManifest.containsKey(normalized) || offlineManifest.containsKey(stripped)
    }

    fun getOfflinePhrasesCount(): Int = offlineManifest.size

    /**
     * Воспроизводит фразу. Сначала ищет студийный запечённый файл, если нет —
     * использует системный TTS.
     */
    fun speak(text: String, onCompletion: (() -> Unit)? = null) {
        if (!soundEnabled || text.isBlank()) return

        val normalized = normalizeKey(text)
        val stripped = normalized.trimEnd('.', '!', '?', ',', ':', ';')
        val fileName = offlineManifest[normalized] ?: offlineManifest[stripped]

        if (fileName != null) {
            playAssetAudio(fileName, onCompletion)
        } else {
            speakViaSystemTts(text)
        }
    }

    private fun playAssetAudio(fileName: String, onCompletion: (() -> Unit)?) {
        try {
            stopAudio()
            val afd = appContext.assets.openFd("audio/ro_a1/$fileName")
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setOnPreparedListener { mp ->
                    _isPlaying.value = true
                    mp.start()
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                    onCompletion?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "Ошибка MediaPlayer: what=$what extra=$extra")
                    _isPlaying.value = false
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось воспроизвести asset: $fileName", e)
            _isPlaying.value = false
        }
    }

    private fun speakViaSystemTts(text: String) {
        if (!ttsReady) return
        stopAudio()
        _isPlaying.value = true
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    /** Останавливает текущую фразу. Вызывается при смене шага и выходе с экрана. */
    fun stop() = stopAudio()

    private fun stopAudio() {
        runCatching {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            }
        }
        mediaPlayer = null
        if (ttsReady) {
            runCatching { tts.stop() }
        }
        _isPlaying.value = false
    }

    fun isRomanianAvailable(): Boolean = offlineManifest.isNotEmpty() || romanianTtsAvailable

    /** Телефон в беззвучном/вибро-режиме: автозапуск аудио лучше не делать. */
    fun isSilentMode(): Boolean {
        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return false
        return audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL
    }

    fun release() {
        stopAudio()
        scope.cancel()
        tts.shutdown()
    }

    private fun normalizeKey(raw: String): String {
        return raw.trim().replace(Regex("\\s+"), " ").lowercase()
    }

    companion object {
        private const val TAG = "AudioPlayer"
        val ROMANIAN: Locale = Locale("ro", "RO")
    }
}
