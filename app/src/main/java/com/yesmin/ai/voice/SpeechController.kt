package com.yesmin.ai.voice
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
class SpeechController(private val context: Context) {
 private var recognizer: SpeechRecognizer? = null
 private val mutableListening = MutableStateFlow(false)
 val listening = mutableListening.asStateFlow()
 private val mutablePartial = MutableStateFlow("")
 val partial = mutablePartial.asStateFlow()
 fun start(language: String, onResult: (String) -> Unit, onError: (String) -> Unit) {
  if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { onError("Microphone permission is required."); return }
  if (!SpeechRecognizer.isRecognitionAvailable(context)) { onError("No speech recognition service. Install/enable a compatible speech service or use text input."); return }
  stop()
  val sr = SpeechRecognizer.createSpeechRecognizer(context)
  recognizer = sr
  sr.setRecognitionListener(object : RecognitionListener {
   override fun onReadyForSpeech(params: Bundle?) { mutableListening.value = true }
   override fun onBeginningOfSpeech() {}
   override fun onRmsChanged(rmsdB: Float) {}
   override fun onBufferReceived(buffer: ByteArray?) {}
   override fun onEndOfSpeech() { mutableListening.value = false }
   override fun onError(error: Int) {
    mutableListening.value = false
    val message = when(error) {
     SpeechRecognizer.ERROR_NO_MATCH -> "No speech matched. Tap the microphone to retry."
     SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
     SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied."
     SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech service network problem. Try text input."
     SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Stop and try again."
     else -> "Speech service error $error. Try again or change the device speech service."
    }
    onError(message)
   }
   override fun onResults(results: Bundle?) {
    mutableListening.value = false
    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onResult)
   }
   override fun onPartialResults(partialResults: Bundle?) {
    mutablePartial.value = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
   }
   override fun onEvent(eventType: Int, params: Bundle?) {}
  })
  mutablePartial.value = ""; mutableListening.value = true
  val tag = when(language) { "hi" -> "hi-IN"; "bn" -> "bn-BD"; else -> "en-IN" }
  try { sr.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
   putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
   putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
   putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
   putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
  }) } catch (_: Exception) { stop(); onError("Unable to start the speech service.") }
 }
 fun stop() { recognizer?.cancel(); recognizer?.destroy(); recognizer = null; mutableListening.value = false }
}
