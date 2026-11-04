package com.yesmin.ai.ui.navigation
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yesmin.ai.R
import com.yesmin.ai.data.AssistantViewModel
import com.yesmin.ai.ui.components.*
import com.yesmin.ai.ui.screens.*
import com.yesmin.ai.ui.theme.YesminTheme
import kotlinx.coroutines.launch
import java.util.Locale
data class Destination(val id:String,val title:Int)
val destinations=listOf(Destination("home",R.string.home),Destination("chat",R.string.chat),Destination("voice",R.string.voice),Destination("apps",R.string.apps),Destination("coding",R.string.coding),Destination("history",R.string.history),Destination("tools",R.string.tools),Destination("settings",R.string.settings),Destination("gemini",R.string.gemini),Destination("privacy",R.string.privacy))
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun YesminApp(vm:AssistantViewModel,minimize:()->Boolean) {
 val data by vm.state.collectAsStateWithLifecycle()
 val host=LocalContext.current
 val baseConfiguration=LocalConfiguration.current
 val config=remember(data.settings.language,baseConfiguration) { Configuration(baseConfiguration).apply {setLocale(Locale.forLanguageTag(data.settings.language))} }
 // Preserve the Activity ContextWrapper chain for permission launchers and dialogs.
 val localized=remember(config) {android.view.ContextThemeWrapper(host,0).apply {applyOverrideConfiguration(config)}}
 CompositionLocalProvider(LocalContext provides localized,LocalConfiguration provides config) {
  YesminTheme(data.settings.theme) {
   var route by rememberSaveable {mutableStateOf("home")}
   val drawer=rememberDrawerState(DrawerValue.Closed)
   val scope=rememberCoroutineScope()
   val snackbar=remember {SnackbarHostState()}
   val error by vm.error.collectAsStateWithLifecycle()
   val preview by vm.pending.collectAsStateWithLifecycle()
   val gesture by vm.pendingGesture.collectAsStateWithLifecycle()
   val gestureRunning by vm.gestureRunning.collectAsStateWithLifecycle()
   val navigate:(String)->Unit={vm.speech.stop();vm.tts.stop();route=it;scope.launch{drawer.close()}}
   LaunchedEffect(error) {error?.let{snackbar.showSnackbar(it);vm.error.value=null}}
   BackHandler(enabled=drawer.isOpen || route!="home") {if(drawer.isOpen)scope.launch{drawer.close()} else {vm.speech.stop();vm.tts.stop();route="home"}}
   ModalNavigationDrawer(drawerState=drawer,drawerContent={
    ModalDrawerSheet(Modifier.widthIn(max=320.dp).verticalScroll(rememberScrollState())) {
     Spacer(Modifier.height(22.dp)); Text("YESMIN AI",Modifier.padding(20.dp),style=MaterialTheme.typography.headlineSmall,color=MaterialTheme.colorScheme.primary)
     destinations.forEach { d ->NavigationDrawerItem(label={Text(stringResource(d.title))},selected=route==d.id,onClick={navigate(d.id)},modifier=Modifier.padding(horizontal=12.dp,vertical=2.dp))}
    }
   }) {
    Scaffold(snackbarHost={SnackbarHost(snackbar)},topBar={
     TopAppBar(title={Column {Text(if(route=="home")"YESMIN AI" else stringResource(destinations.first{it.id==route}.title));Text("PERSONAL INTELLIGENCE",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)}},navigationIcon={IconButton(onClick={scope.launch{drawer.open()}}){Icon(Icons.Default.Menu,contentDescription="Navigation")}},actions={IconButton(onClick={navigate("settings")}){Icon(Icons.Default.Settings,contentDescription="Settings")}})
    }) {padding->
     Box(Modifier.fillMaxSize().padding(padding)) {
      when(route) {
       "home"->HomeScreen(vm,navigate)
       "chat"->ChatScreen(vm)
       "voice"->ChatScreen(vm,voiceMode=true)
       "coding"->ChatScreen(vm,coding=true)
       "apps"->LauncherScreen(vm)
       "history"->HistoryScreen(vm)
       "tools"->ToolsScreen(vm,navigate)
       "settings"->SettingsScreen(vm,navigate)
       "gemini"->GeminiScreen(vm)
       "privacy"->PrivacyScreen(vm)
      }
     }
    }
   }
   if(preview!=null) AlertDialog(onDismissRequest=vm::cancelAction,title={Text(L("Review requested action","कमांड की पुष्टि","কমান্ড নিশ্চিত করুন"))},text={
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
     Text("YESMIN has not performed these actions yet. Only the next action runs when you confirm. Return here to continue any remaining step.")
     preview!!.actions.forEachIndexed { index,a->Text("${index+1}. ${a.type}\n${a.argument}") }
     Text("Inspect website addresses carefully. No messages, purchases, file deletion or sensitive setting changes are automated.",style=MaterialTheme.typography.bodySmall)
    }
   },confirmButton={TextButton(onClick=vm::confirmNextAction){Text("Open next action")}},dismissButton={TextButton(onClick=vm::cancelAction){Text("Cancel all")}})
   if(gesture!=null) AlertDialog(onDismissRequest=vm::cancelAction,title={Text("One-time screen gesture")},text={Text("Requested: ${gesture!!.kind}${if(gesture!!.kind=="TAP") " at ${gesture!!.x}, ${gesture!!.y} px" else ""}.\nAfter a 3-second cancellable countdown, YESMIN will minimize and perform ONE gesture on your previous screen. It cannot inspect or verify that target. Do not continue if the target involves authentication, payments, messages, deletion or sensitive settings. Rotation/keyboard may change coordinates.")},confirmButton={TextButton(onClick={vm.confirmGesture(minimize)}){Text("Confirm one gesture")}},dismissButton={TextButton(onClick=vm::cancelAction){Text("Cancel")}})
   if(gestureRunning) AlertDialog(onDismissRequest=vm::cancelGesture,title={Text("Gesture starts after 3 seconds")},text={Text("YESMIN will minimize to your previous screen. Cancel now to stop. Exactly one gesture is authorized.")},confirmButton={TextButton(onClick=vm::cancelGesture){Text("STOP / CANCEL")}})
  }
 }
}
@Composable private fun HomeScreen(vm:AssistantViewModel,navigate:(String)->Unit) {
 val busy by vm.busy.collectAsStateWithLifecycle()
 val speaking by vm.tts.speaking.collectAsStateWithLifecycle()
 val listening by vm.speech.listening.collectAsStateWithLifecycle()
 val state by vm.state.collectAsStateWithLifecycle()
 val connection by vm.connection.collectAsStateWithLifecycle()
 val status=stringResource(if(listening)R.string.listening else if(busy)R.string.processing else if(speaking)R.string.speaking else R.string.ready)
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
  VoiceOrb(status,busy||speaking||listening,180)
  Text(L("A little help. A lot of possibilities.","आपकी हर दिन की AI सहायक।","প্রতিদিনের কাজে আপনার AI সহকারী।"),style=MaterialTheme.typography.titleMedium)
  Text(connection,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.secondary)
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
   Button(onClick={navigate("voice")},modifier=Modifier.weight(1f)){Icon(Icons.Default.Mic,null);Spacer(Modifier.width(6.dp));Text(stringResource(R.string.voice))}
   FilledTonalButton(onClick={navigate("chat")},modifier=Modifier.weight(1f)){Text(stringResource(R.string.chat))}
  }
  if(!state.settings.cloudConsent) Section(L("Start safely","सुरक्षित शुरुआत","নিরাপদ শুরু")) {
   Text("Local tools work without cloud AI. For Gemini chat, first configure Firebase and review what is sent to the cloud.")
   Row {TextButton(onClick={navigate("gemini")}){Text("Configure Gemini")};TextButton(onClick={navigate("privacy")}){Text("Review privacy")}}
  }
  listOf("apps","coding","tools","history","settings").forEach {id->
   Card(Modifier.fillMaxWidth().clickable{navigate(id)}) {
    Row(Modifier.fillMaxWidth().padding(20.dp),verticalAlignment=Alignment.CenterVertically) {
     Text(stringResource(destinations.first{it.id==id}.title),Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
     Icon(Icons.Default.ChevronRight,null,tint=MaterialTheme.colorScheme.primary)
    }
   }
  }
  Text("YESMIN is an AI assistant, not a human. Verify important information.",style=MaterialTheme.typography.labelSmall)
 }
}
