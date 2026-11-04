package com.yesmin.ai.ui.screens
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.FirebaseApp
import com.yesmin.ai.BuildConfig
import com.yesmin.ai.accessibility.YesminAccessibilityService
import com.yesmin.ai.data.AssistantViewModel
import com.yesmin.ai.data.UserSettings
import com.yesmin.ai.ui.components.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
@Composable fun SettingsScreen(vm:AssistantViewModel,navigate:(String)->Unit) {
 val data by vm.state.collectAsStateWithLifecycle()
 val s=data.settings
 val voices by vm.tts.voices.collectAsStateWithLifecycle()
 var reset by remember { mutableStateOf(false) }
 var clear by remember { mutableStateOf(false) }
 val context=LocalContext.current
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  Section("Gemini") { Button(onClick={navigate("gemini")}) { Text(L("Gemini configuration","Gemini कॉन्फ़िगरेशन","Gemini কনফিগারেশন")) } }
  Section(L("Language & appearance","भाषा और थीम","ভাষা ও থিম")) {
   Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { listOf("en" to "English","hi" to "हिन्दी","bn" to "বাংলা").forEach { (code,label) -> FilterChip(s.language==code,{vm.setSettings { it.copy(language=code,voiceName="") }},label={Text(label)}) } }
   Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { listOf("dark","light","system").forEach { mode -> FilterChip(s.theme==mode,{vm.setSettings { it.copy(theme=mode) }},label={Text(mode.replaceFirstChar(Char::uppercase))}) } }
  }
  Section(L("Voice","आवाज़","কণ্ঠস্বর")) {
   Toggle("Enable voice input/output",s.voiceEnabled,{v->vm.setSettings{it.copy(voiceEnabled=v)}; if(!v){vm.speech.stop();vm.tts.stop()}})
   Toggle("Speak replies automatically",s.autoSpeak,{v->vm.setSettings{it.copy(autoSpeak=v)}})
   Text("Speech speed: ${"%.1f".format(s.speechRate)}×")
   Slider(s.speechRate,{v->vm.setSettings{it.copy(speechRate=v)}},valueRange=0.5f..1.8f)
   var showVoices by remember { mutableStateOf(false) }
   OutlinedButton(onClick={showVoices=true}) { Text("Voice: ${s.voiceName.ifBlank { "Device default / female-labelled if available" }}") }
   if(showVoices) AlertDialog(onDismissRequest={showVoices=false},title={Text("Installed TTS voices")},text={
    Column(Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState())) {
     TextButton(onClick={vm.setSettings{it.copy(voiceName="")};showVoices=false}) {Text("Automatic")}
     voices.forEach { voice -> TextButton(onClick={vm.setSettings{it.copy(voiceName=voice.name)};showVoices=false}) {Text(voice.label)} }
     if(voices.isEmpty()) Text("No voices loaded. Install/enable an Android TTS engine and restart the app.")
    }
   },confirmButton={TextButton(onClick={showVoices=false}){Text("Close")}})
   Text("Android does not reliably label voice gender. Choose and preview a voice you prefer. Network voices can send speech text to the engine provider.",style=MaterialTheme.typography.bodySmall)
   Row { TextButton(onClick={vm.speak(when(s.language){"hi"->"नमस्ते, मैं आपकी एआई सहायक येसमिन हूँ।";"bn"->"হ্যালো, আমি আপনার এআই সহকারী ইয়েসমিন।";else->"Hello, I am Yesmin, your AI assistant."})}){Text("Preview voice")}
    TextButton(onClick={runCatching{context.startActivity(Intent("com.android.settings.TTS_SETTINGS"))}.onFailure{vm.notifyError("Open Android Settings → Text-to-speech manually.")}}){Text("Voice data")}
   }
  }
  Section(L("Memory you control","आपके नियंत्रण में जानकारी","আপনার নিয়ন্ত্রণে তথ্য")) {
   Toggle("Save chat and command history on this phone",s.saveHistory,{v->vm.setSettings{it.copy(saveHistory=v)}},"Off by default. Turning off removes persisted chat/command/app history; the current session stays until cleared.")
   Toggle("Use recent chat as AI context",s.useContext,{v->vm.setSettings{it.copy(useContext=v)}})
   var prefs by remember(s.preferences) { mutableStateOf(s.preferences) }
   OutlinedTextField(prefs,{prefs=it.take(2000)},label={Text("Approved preferences only")},modifier=Modifier.fillMaxWidth(),minLines=3)
   Button(onClick={vm.setSettings{it.copy(preferences=prefs)}}){Text("Save preferences locally")}
   Toggle("Include saved preferences in Gemini requests",s.sharePreferences,{v->vm.setSettings{it.copy(sharePreferences=v)}})
   TextButton(onClick={vm.setSettings{it.copy(preferences="",sharePreferences=false)}}){Text("Forget preferences")}
   TextButton(onClick={clear=true}){Text("Clear chat & command history")}
  }
  Section(L("Privacy & diagnostics","गोपनीयता और सहायता","গোপনীয়তা ও সহায়তা")) {
   Button(onClick={navigate("privacy")}){Text("Permissions & privacy")}
   Text("Errors stay in this session. No analytics, crash-upload SDK or prompt logging is included.")
   TextButton(onClick={shareText(context,Json.encodeToString(s.copy(preferences="",sharePreferences=false,cloudConsent=false)))}){Text("Share non-secret settings (preferences excluded)")}
  }
  Section("About YESMIN AI") {
   Text("Native Android · Kotlin · Compose · Firebase AI Logic\nVersion ${BuildConfig.VERSION_NAME}\nYESMIN is an AI, not a human. Verify important answers. No background wake-word listener.")
   TextButton(onClick={reset=true}){Text("Reset local settings and all local data")}
  }
 }
 if(clear) AlertDialog(onDismissRequest={clear=false},title={Text("Clear local history")},text={Text("Chat, command history and recent apps will be removed. This cannot delete provider-side data.")},confirmButton={TextButton(onClick={vm.clearHistory();clear=false}){Text("Clear")}},dismissButton={TextButton(onClick={clear=false}){Text("Cancel")}})
 if(reset) AlertDialog(onDismissRequest={reset=false},title={Text("Reset all local data")},text={Text("Deletes local messages, notes, preferences, shortcuts and reminders, cancels alarms and disables this accessibility service. Firebase project configuration remains compiled into the app.")},confirmButton={TextButton(onClick={vm.resetAll();reset=false}){Text("Reset")}},dismissButton={TextButton(onClick={reset=false}){Text("Cancel")}})
}
@Composable fun GeminiScreen(vm:AssistantViewModel) {
 val data by vm.state.collectAsStateWithLifecycle()
 val connection by vm.connection.collectAsStateWithLifecycle()
 val busy by vm.busy.collectAsStateWithLifecycle()
 val attempts by vm.requestCount.collectAsStateWithLifecycle()
 val successes by vm.successfulRequests.collectAsStateWithLifecycle()
 val error by vm.lastError.collectAsStateWithLifecycle()
 val context=LocalContext.current
 var model by remember(data.settings.model) { mutableStateOf(data.settings.model) }
 var reset by remember { mutableStateOf(false) }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  Section("Firebase AI Logic · Gemini") {
   Text(connection,color=MaterialTheme.colorScheme.secondary)
   val options=remember { FirebaseApp.getApps(context).firstOrNull()?.options }
   Text("Project: ${options?.projectId ?: "Not configured"}")
   Text("Build: ${if(BuildConfig.DEBUG) "Debug App Check provider" else "Play Integrity App Check provider"}")
   Toggle("Connect Gemini",data.settings.aiEnabled,{v->if(v)vm.setSettings{it.copy(aiEnabled=true)} else vm.disconnect()})
   Text("Requires internet, Firebase AI Logic setup and registered App Check tokens. Local connection status is not proof of backend enforcement.")
  }
  Section("Model selection") {
   listOf("gemini-3.8-flash","gemini-3.5-flash-lite").forEach { name ->
    OutlinedButton(onClick={model=name;vm.setSettings{it.copy(model=name)}}) {Text(name)}
   }
   OutlinedTextField(model,{model=it.take(80)},label={Text("Supported Gemini model ID")},modifier=Modifier.fillMaxWidth(),singleLine=true)
   Button(onClick={if(model.matches(Regex("gemini-[a-zA-Z0-9.-]{1,64}"))) vm.setSettings{it.copy(model=model)} else vm.notifyError("Enter a valid gemini- model ID.")}){Text("Save model")}
   Text("Availability changes by project, region and provider. If a model is retired, enter an available text-generation model from Firebase documentation. Test before use.",style=MaterialTheme.typography.bodySmall)
  }
  Section("API test & usage") {
   Button(onClick=vm::testConnection,enabled=!busy){Text(if(busy)"Testing/request in progress…" else "Test Firebase → Gemini connection")}
   Text("Session attempts: $attempts · successes: $successes\nNot a billing/token meter. Use Firebase/Google Cloud console for actual quota and spend.")
   if(error!=null) Text(error!!,color=MaterialTheme.colorScheme.error)
   TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://console.firebase.google.com/")))}.onFailure{vm.notifyError("No browser available.")}}){Text("Open Firebase console")}
  }
  Section("Configuration management") {
   Text("1. Register Android app com.yesmin.ai in your Firebase project.\n2. Enable Firebase AI Logic with the Gemini Developer API.\n3. Place your downloaded google-services.json in app/ and rebuild.\n4. Register the debug App Check token for personal testing; use Play Integrity for production.\n5. Enable Firebase AI Logic App Check enforcement in the console.\nFull instructions are in FIREBASE_SETUP.md.")
   Text("The Firebase client config is not a Gemini secret. Do not put a Gemini API key in the source or APK. This app intentionally has no personal API-key input.")
   TextButton(onClick=vm::disconnect){Text("Disconnect locally")}
   TextButton(onClick={reset=true}){Text("Reset Gemini settings")}
  }
 }
 if(reset) AlertDialog(onDismissRequest={reset=false},title={Text("Reset Gemini configuration")},text={Text("Stops requests, disconnects AI, resets model and revokes cloud consent. To change/remove the compiled Firebase project, replace/remove app/google-services.json and rebuild.")},confirmButton={TextButton(onClick={vm.disconnect();vm.setSettings{it.copy(model=UserSettings().model,cloudConsent=false)};reset=false}){Text("Reset")}},dismissButton={TextButton(onClick={reset=false}){Text("Cancel")}})
}
@Composable fun PrivacyScreen(vm:AssistantViewModel) {
 val data by vm.state.collectAsStateWithLifecycle()
 val context=LocalContext.current
 val owner=LocalLifecycleOwner.current
 var statusVersion by remember { mutableIntStateOf(0) }
 DisposableEffect(owner) {
  val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_RESUME) statusVersion++ }
  owner.lifecycle.addObserver(observer); onDispose {owner.lifecycle.removeObserver(observer)}
 }
 val enabled=remember(statusVersion,data.settings.screenEnabled){YesminAccessibilityService.instance!=null}
 var disclosure by remember { mutableStateOf(false) }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  Section(L("Cloud AI consent","Cloud AI अनुमति","Cloud AI সম্মতি")) {
   Text("When you send an AI request, your message, up to 16 recent messages (if context is enabled) and explicitly shared preferences are sent via Firebase AI Logic to Google's selected Gemini backend. Provider processing/retention, quota and billing policies apply. No screen text, app inventory, notes or clipboard are uploaded automatically.")
   Toggle("I consent to cloud AI processing",data.settings.cloudConsent,{v->if(!v)vm.cancelRequest();vm.setSettings{it.copy(cloudConsent=v)}})
   Text("Do not include passwords, authentication codes, private keys, banking details or information you do not want processed in the cloud. Clear local history does not erase provider-side records.")
  }
  Section(L("Microphone & speech","माइक्रोफ़ोन और आवाज़","মাইক্রোফোন ও কণ্ঠস্বর")) {
   val granted=ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED
   Text("Microphone permission: ${if(granted) "granted" else "not granted"}")
   Text("Listening starts only after a microphone tap and ends on Stop, recognition completion or leaving the app. No background recording or call recording. Android's recognition provider may process audio on its servers; its policy applies. TTS network voices may upload the spoken text. Text input works without microphone access.")
   Toggle("Allow voice features",data.settings.voiceEnabled,{v->vm.setSettings{it.copy(voiceEnabled=v)};if(!v){vm.speech.stop();vm.tts.stop()}})
   TextButton(onClick={runCatching{context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}")))}.onFailure{vm.notifyError("Open Android App settings manually.")}}){Text("Android app permissions")}
  }
  Section(L("Optional Screen Control","वैकल्पिक स्क्रीन कंट्रोल","ঐচ্ছিক স্ক্রিন কন্ট্রোল")) {
   Text("Service: ${if(enabled) "connected" else "disabled/disconnected"} · app gate: ${if(data.settings.screenEnabled) "on" else "off"}")
   Text("YESMIN is a general assistant, not a disability accessibility tool. The optional service can perform taps, swipes, Back, Home and Recents. It has no screen-text retrieval permission and never reads password fields. Gemini cannot request or execute gestures.")
   Text("Each exact user gesture command requires confirmation. After a cancellable 3-second countdown, YESMIN minimizes and performs ONE gesture on the previous screen. You must inspect that target yourself. Never use this on authentication, payment, purchases, messaging, deletion or other consequential controls. Android may block protected screens. Coordinates are full-screen pixels, not dp.")
   Text("Supported examples: swipe up · swipe down · go back · go home · open recent apps · tap 300,600")
   Button(onClick={disclosure=true}){Text("Review & open Accessibility settings")}
   OutlinedButton(onClick={vm.cancelGesture();vm.setSettings{it.copy(screenEnabled=false)};YesminAccessibilityService.instance?.disableSelf();statusVersion++}){Text("Disable screen control immediately")}
   Text("To revoke access fully, also turn off YESMIN Screen Control in Android Accessibility settings. Enabling the OS service alone does not enable the in-app consent gate.")
  }
  Section(L("Local data & clipboard","स्थानीय डेटा और क्लिपबोर्ड","স্থানীয় তথ্য ও ক্লিপবোর্ড")) {
   Text("Chat retention is off by default. Notes, reminders, favourites, shortcuts, settings and explicitly saved preferences are app-private local data. Android sandbox protection applies; this v1 does not add a separate encrypted vault. App backups are disabled. Avoid saving highly sensitive information.")
   Text("Clipboard is read only when you press the explicit Paste clipboard button in Notes & Tools. Pasting alone does not send it to Gemini. Installed launcher apps are queried only for the app launcher; no QUERY_ALL_PACKAGES permission is requested. No analytics, ads, hidden screen monitoring, background microphone or shell execution is included.")
  }
  Section("Policy & troubleshooting") {
   Text("App Check protects the backend only after you configure and enforce it in Firebase. It is not a substitute for quotas and spend controls. A sideloaded production release may fail Play Integrity attestation. Use registered debug builds only for your private tests. Google Play accessibility declaration/policy review is your responsibility; do not claim this is a disability tool.")
   Text("For errors: verify internet, speech/TTS engine, Firebase setup, App Check registration, model availability, notification settings and Android battery restrictions. Review TESTING.md before trusting device actions.")
  }
 }
 if(disclosure) AlertDialog(onDismissRequest={disclosure=false},title={Text("Optional accessibility access")},text={Text("This service can interact with other apps through explicit one-time gestures. It does not retrieve screen content or choose targets. Never request clicks on sensitive controls. Android will show a separate enable switch. You can disable both the service and the app gate anytime. Continue only if you understand and trust this access.")},confirmButton={TextButton(onClick={
  vm.setSettings{it.copy(screenEnabled=true)};disclosure=false
  runCatching{context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}.onFailure{vm.notifyError("Open Android Accessibility settings manually.")}
 }){Text("I understand · open settings")}},dismissButton={TextButton(onClick={disclosure=false}){Text("Cancel")}})
}
