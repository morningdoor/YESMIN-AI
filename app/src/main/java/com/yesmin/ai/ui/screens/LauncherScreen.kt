package com.yesmin.ai.ui.screens
import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yesmin.ai.data.AssistantViewModel
import com.yesmin.ai.ui.components.L
@Composable fun LauncherScreen(vm:AssistantViewModel) {
 val apps by vm.installedApps.collectAsStateWithLifecycle()
 val state by vm.state.collectAsStateWithLifecycle()
 var search by rememberSaveable { mutableStateOf("") }
 var filter by rememberSaveable { mutableStateOf("All") }
 var microphoneDisclosure by remember { mutableStateOf(false) }
 val listening by vm.speech.listening.collectAsStateWithLifecycle()
 val partial by vm.speech.partial.collectAsStateWithLifecycle()
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if(it) vm.startListening { s->search=s } else vm.notifyError("Microphone denied.") }
 Column(Modifier.fillMaxSize().padding(horizontal=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Row(verticalAlignment=Alignment.CenterVertically) {
   OutlinedTextField(search,{search=it},modifier=Modifier.weight(1f),label={Text(L("Search installed apps","ऐप खोजें","অ্যাপ খুঁজুন"))},singleLine=true)
   IconButton(onClick={if(listening)vm.speech.stop() else microphoneDisclosure=true},enabled=state.settings.voiceEnabled) { Icon(if(listening)Icons.Default.Stop else Icons.Default.Mic,contentDescription="Voice search / stop") }
   IconButton(onClick=vm::refreshApps) { Icon(Icons.Default.Refresh,contentDescription="Refresh apps") }
  }
  if(listening) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(partial.ifBlank { "Listening · tap Stop to cancel" }) }
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("All","Favourites","Recent").forEach { mode -> FilterChip(selected=mode==filter,onClick={filter=mode},label={Text(mode)}) } }
  val filtered=apps.filter { it.name.contains(search,true) || it.packageName.contains(search,true) }
   .filter { when(filter) { "Favourites" -> it.packageName in state.favourites; "Recent" -> it.packageName in state.recentApps; else -> true } }
   .let { if(filter=="Recent") it.sortedBy { app->state.recentApps.indexOf(app.packageName) } else it }
  if(filtered.isEmpty()) Text(L("No matching launcher apps. Try refresh.","कोई ऐप नहीं मिला। रीफ़्रेश करें।","কোনো অ্যাপ পাওয়া যায়নি। রিফ্রেশ করুন।"))
  LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)) {
   items(filtered,key={it.packageName}) { app ->
    ListItem(headlineContent={Text(app.name)},supportingContent={Text(app.packageName,style=MaterialTheme.typography.labelSmall)},
     leadingContent={
      val bitmap=remember(app.packageName) { Bitmap.createBitmap(72,72,Bitmap.Config.ARGB_8888).also { b-> app.icon.setBounds(0,0,72,72); app.icon.draw(Canvas(b)) }.asImageBitmap() }
      Image(bitmap,contentDescription=null,modifier=Modifier.size(40.dp))
     },trailingContent={IconButton(onClick={vm.toggleFavourite(app.packageName)}) {Icon(if(app.packageName in state.favourites) Icons.Default.Star else Icons.Default.StarOutline,contentDescription="Toggle favourite")}},
     modifier=Modifier.clickable { vm.launchApp(app) })
   }
  }
 }
 if(microphoneDisclosure) AlertDialog(onDismissRequest={microphoneDisclosure=false},title={Text("Voice app search")},text={Text("Android's speech provider may process your audio on its servers. YESMIN listens only while foregrounded and stops after recognition or when you tap Stop. The result filters this local launcher list; it is not sent to Gemini and does not open an app automatically.")},confirmButton={TextButton(onClick={microphoneDisclosure=false;permission.launch(Manifest.permission.RECORD_AUDIO)}){Text("Continue")}},dismissButton={TextButton(onClick={microphoneDisclosure=false}){Text("Cancel")}})
}
