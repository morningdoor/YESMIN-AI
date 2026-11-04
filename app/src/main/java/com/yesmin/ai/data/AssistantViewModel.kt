package com.yesmin.ai.data
import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yesmin.ai.accessibility.YesminAccessibilityService
import com.yesmin.ai.ai.GeminiRepository
import com.yesmin.ai.apps.AppRepository
import com.yesmin.ai.apps.InstalledApp
import com.yesmin.ai.commands.*
import com.yesmin.ai.reminders.ReminderScheduler
import com.yesmin.ai.voice.SpeechController
import com.yesmin.ai.voice.TtsController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Locale
data class ActionPreview(val original: String, val actions: List<ProposedAction>, val createdAt: Long = SystemClock.elapsedRealtime())
class AssistantViewModel(application: Application) : AndroidViewModel(application) {
 val store=LocalStore(application)
 val state=store.state
 val gemini=GeminiRepository(application)
 val appRepository=AppRepository(application,store)
 val speech=SpeechController(application)
 val tts=TtsController(application)
 private val router=CommandRouter(application,appRepository)
 private val reminders=ReminderScheduler(application)
 val installedApps=MutableStateFlow<List<InstalledApp>>(emptyList())
 val busy=MutableStateFlow(false)
 val error=MutableStateFlow<String?>(null)
 val lastError=MutableStateFlow<String?>(null)
 val connection=MutableStateFlow(if(gemini.configured()) "Configured · not tested" else "Firebase configuration missing")
 val requestCount=MutableStateFlow(0)
 val successfulRequests=MutableStateFlow(0)
 val pending=MutableStateFlow<ActionPreview?>(null)
 val pendingGesture=MutableStateFlow<GestureRequest?>(null)
 val gestureRunning=MutableStateFlow(false)
 val selectedApp=MutableStateFlow<InstalledApp?>(null)
 private var activeJob: Job? = null
 private var gestureJob: Job? = null
 private var foreground=true
 private var gestureMinimizing=false
 init { refreshApps() }
 fun refreshApps() { viewModelScope.launch {
  installedApps.value=withContext(Dispatchers.IO) { appRepository.refresh() }
 } }
 fun setSettings(block: (UserSettings)->UserSettings) { store.settings(block) }
 fun notifyError(message: String) { val safe=message.take(600); error.value=safe; lastError.value=safe }
 private fun friendlyError(e: Throwable): String = when {
  !gemini.configured() -> "Firebase is not configured. Follow FIREBASE_SETUP.md and rebuild."
  !state.value.settings.cloudConsent -> "Cloud AI is off. Review and enable consent in Privacy & Permissions."
  !state.value.settings.aiEnabled -> "Gemini is disconnected. Reconnect in Gemini Configuration."
  e is TimeoutCancellationException -> "Gemini timed out. Check internet and retry."
  e is IllegalArgumentException -> "Gemini returned an unsupported response or configuration. Check the model ID and try again."
  else -> "Gemini request failed (${e.javaClass.simpleName}). Check internet, model availability, Firebase AI Logic setup, App Check registration/enforcement, quota and billing. No credentials or prompts were logged."
 }
 fun send(raw: String, coding: Boolean=false) {
  if(busy.value || gestureRunning.value) return
  val text=raw.trim()
  if(text.isEmpty()) return
  if(text.length>12000) { notifyError("Please limit one message to 12,000 characters."); return }
  if(pending.value!=null || pendingGesture.value!=null) { notifyError("Review or cancel the pending action first."); return }
  tts.stop(); speech.stop()
  store.addMessage("user",text)
  // A gesture can originate only from the exact current user text, never a Gemini response or shortcut.
  LocalCommandParser.gesture(text)?.let {
   if(!state.value.settings.screenEnabled || YesminAccessibilityService.instance==null) {
    store.addMessage("assistant","Screen Control is off. Read its disclosure in Privacy, then manually enable the Android Accessibility service if you want to use it.")
   } else pendingGesture.value=it
   return
  }
  val effective=state.value.shortcuts.firstOrNull { it.phrase.equals(text,true) }?.command ?: text
  val local=if(coding) null else LocalCommandParser.parse(effective)
  if(local!=null) { acceptReply(text,local); return }
  busy.value=true
  activeJob=viewModelScope.launch {
   try {
    requestCount.value++
    val history=state.value.messages.dropLast(1)
    val reply=withTimeout(60000) { gemini.ask(state.value.settings,history,effective,coding) }
    successfulRequests.value++; connection.value="Connected · last request succeeded"
    acceptReply(text,reply)
   } catch(e: CancellationException) {
    if(e is TimeoutCancellationException) { val msg=friendlyError(e); notifyError(msg); store.addMessage("assistant",msg) }
    else throw e
   } catch(e: Exception) {
    val msg=friendlyError(e); connection.value="Connection/request error"; notifyError(msg); store.addMessage("assistant",msg); store.log(text,"AI request failed")
   } finally { busy.value=false }
  }
 }
 private fun acceptReply(original: String,reply: BrainReply) {
  val valid=ActionValidator.validate(reply.actions)
  if(valid.isFailure) { notifyError(valid.exceptionOrNull()?.message ?: "Unsafe action rejected"); return }
  store.addMessage("assistant",reply.reply)
  if(reply.actions.isNotEmpty()) pending.value=ActionPreview(original,reply.actions)
  else store.log(original,"Answered")
  val settings=state.value.settings
  if(foreground && settings.voiceEnabled && settings.autoSpeak) tts.speak(reply.reply,settings)
 }
 fun confirmNextAction() {
  val preview=pending.value ?: return
  if(SystemClock.elapsedRealtime()-preview.createdAt>120000) { cancelAction(); notifyError("Action confirmation expired. Send the request again."); return }
  val action=preview.actions.firstOrNull() ?: return
  pending.value=null
  try {
   val result=router.execute(action)
   store.log(preview.original,result); store.addMessage("assistant",result)
   if(preview.actions.size>1) pending.value=preview.copy(actions=preview.actions.drop(1))
  } catch(e: Exception) {
   val msg=e.message?.take(350) ?: "Android could not open that target."
   notifyError(msg); store.log(preview.original,"Failed: $msg"); store.addMessage("assistant",msg)
  }
 }
 fun cancelAction() { pending.value?.let { store.log(it.original,"Cancelled") }; pending.value=null; pendingGesture.value=null }
 fun launchApp(app: InstalledApp) {
  try { val result=appRepository.launch(app); store.log("Open ${app.name}",result) }
  catch(_: Exception) { notifyError("Unable to launch ${app.name}; it may be disabled or unavailable.") }
 }
 fun confirmGesture(minimize: ()->Boolean) {
  val request=pendingGesture.value ?: return
  pendingGesture.value=null
  if(!state.value.settings.screenEnabled || YesminAccessibilityService.instance==null) { notifyError("Enable Screen Control in Privacy first."); return }
  gestureRunning.value=true
  gestureJob=viewModelScope.launch {
   try {
    // Keep this app visible during a cancellable countdown, then expose the previous foreground app.
    repeat(3) { delay(1000) }
    val consentAt=SystemClock.elapsedRealtime()
    gestureMinimizing=true
    if(!minimize()) { notifyError("Unable to show the previous app. Gesture cancelled."); return@launch }
    delay(600)
    val service=YesminAccessibilityService.instance
    val ok=if(!foreground && state.value.settings.screenEnabled && service!=null) withTimeout(3000) { service.perform(request,consentAt) } else false
    store.log("Screen ${request.kind}",if(ok) "One requested gesture completed" else "Gesture rejected or cancelled")
    if(!ok) notifyError("Android rejected/cancelled the gesture. Coordinates, service status or the target window may be unsupported.")
   } catch(e: CancellationException) { if(e is TimeoutCancellationException) notifyError("Gesture timed out.") else throw e }
   finally { gestureRunning.value=false; gestureMinimizing=false }
  }
 }
 fun cancelGesture() { gestureJob?.cancel(); pendingGesture.value=null; gestureRunning.value=false }
 fun startListening(onText: (String)->Unit) {
  if(!state.value.settings.voiceEnabled) { notifyError("Voice is disabled in Settings."); return }
  if(busy.value || gestureRunning.value) return
  tts.stop(); speech.start(state.value.settings.language,onText,::notifyError)
 }
 fun speak(text:String) {
  if(!state.value.settings.voiceEnabled) { notifyError("Voice is disabled in Settings."); return }
  speech.stop(); tts.speak(text,state.value.settings)?.let(::notifyError)
 }
 fun onForeground(value:Boolean) {
  foreground=value
  if(!value) {
   speech.stop(); tts.stop()
   if(gestureRunning.value && !gestureMinimizing) { cancelGesture(); notifyError("Gesture cancelled because YESMIN left the foreground during confirmation countdown.") }
  }
 }
 fun testConnection() {
  if(busy.value) return
  busy.value=true
  activeJob=viewModelScope.launch {
   try {
    requestCount.value++
    withTimeout(30000) { gemini.test(state.value.settings) }
    successfulRequests.value++; connection.value="Connected · API test succeeded"
   } catch(e: CancellationException) { if(e is TimeoutCancellationException) { connection.value="Test timed out"; notifyError(friendlyError(e)) } else throw e }
   catch(e: Exception) { connection.value="Test failed"; notifyError(friendlyError(e)) }
   finally { busy.value=false }
  }
 }
 fun disconnect() { cancelRequest(); setSettings { it.copy(aiEnabled=false) }; connection.value="Disconnected locally · project configuration retained" }
 fun cancelRequest() { activeJob?.cancel(); busy.value=false; speech.stop(); tts.stop() }
 fun clearHistory() { cancelRequest(); cancelAction(); store.clearHistory() }
 fun toggleFavourite(pkg:String) = store.update { it.copy(favourites=if(pkg in it.favourites) it.favourites-pkg else it.favourites+pkg) }
 fun toggleFavouriteCommand(command:String) = store.update { it.copy(favouriteCommands=if(command in it.favouriteCommands) it.favouriteCommands-command else (it.favouriteCommands+command).takeLast(30)) }
 fun addShortcut(phrase:String,command:String) {
  require(phrase.isNotBlank() && command.isNotBlank() && phrase.length<=120 && command.length<=1000) { "Invalid shortcut" }
  require(LocalCommandParser.gesture(command)==null) { "Screen gestures cannot be shortcut targets" }
  store.update { it.copy(shortcuts=(it.shortcuts.filterNot { s->s.phrase.equals(phrase,true) }+Shortcut(phrase.trim(),command.trim())).takeLast(30)) }
 }
 fun removeShortcut(s: Shortcut) = store.update { it.copy(shortcuts=it.shortcuts-s) }
 fun addNote(text:String) { if(text.isNotBlank()) store.update { it.copy(notes=(it.notes+Note(text=text.trim().take(10000))).takeLast(100)) } }
 fun deleteNote(note:Note) = store.update { it.copy(notes=it.notes-note) }
 fun addReminder(text:String,date:String) {
  require(text.isNotBlank()) { "Enter reminder text" }
  val parser=SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).apply { isLenient=false }
  val position=java.text.ParsePosition(0)
  val parsed=parser.parse(date.trim(),position)
  require(parsed!=null && position.index==date.trim().length) { "Use local time yyyy-MM-dd HH:mm" }
  val at=parsed.time
  val used=state.value.reminders.map { it.id }.toSet()
  var id=(System.currentTimeMillis() and 0x7fffffff).toInt()
  while(id in used) id++
  val r=Reminder(id,text.trim().take(500),at)
  reminders.schedule(r)
  store.update { it.copy(reminders=(it.reminders+r).sortedBy { r->r.at }) }
 }
 fun deleteReminder(r:Reminder) { reminders.cancel(r); store.update { it.copy(reminders=it.reminders-r) } }
 fun resetAll() {
  cancelRequest(); cancelGesture(); cancelAction()
  state.value.reminders.forEach(reminders::cancel)
  YesminAccessibilityService.instance?.disableSelf()
  store.reset(); lastError.value=null; connection.value=if(gemini.configured()) "Configured · not tested" else "Firebase configuration missing"
 }
 override fun onCleared() { cancelGesture(); speech.stop(); tts.close(); super.onCleared() }
}
