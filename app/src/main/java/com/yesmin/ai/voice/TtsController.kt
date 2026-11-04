package com.yesmin.ai.voice
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.yesmin.ai.data.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
data class VoiceOption(val name: String, val label: String)
class TtsController(context: Context) {
 private val handler = Handler(Looper.getMainLooper())
 private var ready = false
 private var engine: TextToSpeech? = null
 private val mutableSpeaking = MutableStateFlow(false)
 val speaking = mutableSpeaking.asStateFlow()
 private val mutableVoices = MutableStateFlow<List<VoiceOption>>(emptyList())
 val voices = mutableVoices.asStateFlow()
 init {
  engine = TextToSpeech(context) { status ->
   ready = status == TextToSpeech.SUCCESS
   if(ready) handler.post {
    mutableVoices.value = engine?.voices.orEmpty().sortedBy { it.name }.map { VoiceOption(it.name, "${it.locale.displayName} · ${it.name}${if(it.isNetworkConnectionRequired) " (network)" else " (offline)"}") }
    engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
     override fun onStart(utteranceId: String?) { mutableSpeaking.value = true }
     override fun onDone(utteranceId: String?) { mutableSpeaking.value = false }
     @Deprecated("Legacy callback") override fun onError(utteranceId: String?) { mutableSpeaking.value = false }
    })
   }
  }
 }
 fun speak(text: String, settings: UserSettings): String? {
  if(!ready) return "Text-to-speech engine is not ready. Install/enable a TTS engine in Android settings."
  val tts = engine ?: return "No TTS engine"
  val locale = when(settings.language) { "hi" -> Locale.forLanguageTag("hi-IN"); "bn" -> Locale.forLanguageTag("bn-BD"); else -> Locale.forLanguageTag("en-IN") }
  val langStatus = tts.setLanguage(locale)
  if(langStatus == TextToSpeech.LANG_MISSING_DATA || langStatus == TextToSpeech.LANG_NOT_SUPPORTED) return "This TTS language is not installed/supported. Install voice data or select another language."
  val available = tts.voices.orEmpty().filter { it.locale.language == locale.language }
  // Android Voice has no standard gender field. Prefer female-labelled voices where present, but do not promise gender.
  val chosen: Voice? = available.firstOrNull { it.name == settings.voiceName }
   ?: available.firstOrNull { it.name.contains("female", true) && !it.isNetworkConnectionRequired }
  if(chosen != null) tts.voice = chosen
  tts.setSpeechRate(settings.speechRate.coerceIn(0.5f, 1.8f))
  val readable = text.replace(Regex("```[\\s\\S]*?```"), "Code is available on screen.").replace(Regex("[*#`]"), "").take(3500)
  return if(tts.speak(readable, TextToSpeech.QUEUE_FLUSH, null, "yesmin-${System.nanoTime()}") == TextToSpeech.ERROR) "Speech output failed." else null
 }
 fun stop() { engine?.stop(); mutableSpeaking.value = false }
 fun close() { stop(); engine?.shutdown(); engine = null }
}
