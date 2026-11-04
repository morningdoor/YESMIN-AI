package com.yesmin.ai.ui.screens
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yesmin.ai.R
import com.yesmin.ai.data.AssistantViewModel
import com.yesmin.ai.ui.components.*
@Composable fun ChatScreen(vm:AssistantViewModel,voiceMode:Boolean=false,coding:Boolean=false) {
 val saved by vm.state.collectAsStateWithLifecycle()
 val busy by vm.busy.collectAsStateWithLifecycle()
 val listening by vm.speech.listening.collectAsStateWithLifecycle()
 val speaking by vm.tts.speaking.collectAsStateWithLifecycle()
 val partial by vm.speech.partial.collectAsStateWithLifecycle()
 var input by rememberSaveable { mutableStateOf("") }
 var clearConfirm by remember { mutableStateOf(false) }
 var microphoneDisclosure by remember { mutableStateOf(false) }
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
  if(granted) vm.startListening { vm.send(it,coding) } else vm.notifyError("Microphone denied. Use text input or grant permission in Android App settings.")
 }
 val listState=rememberLazyListState()
 LaunchedEffect(saved.messages.size) { if(saved.messages.isNotEmpty()) listState.animateScrollToItem(saved.messages.lastIndex) }
 val status=stringResource(if(listening) R.string.listening else if(busy) R.string.processing else if(speaking) R.string.speaking else R.string.ready)
 Column(Modifier.fillMaxSize().imePadding(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
   Text(if(coding) L("Generate · explain · debug","बनाएँ · समझें · डिबग","লিখুন · বুঝুন · ডিবাগ") else L("Your personal AI companion","आपकी AI सहायक","আপনার AI সহকারী"),Modifier.weight(1f),style=MaterialTheme.typography.labelLarge)
   IconButton(onClick={clearConfirm=true}) { Icon(Icons.Default.DeleteSweep,contentDescription="Clear conversation") }
  }
  if(voiceMode) Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) { VoiceOrb(status,listening || speaking || busy,160) }
  else if(busy || listening || speaking) LinearProgressIndicator(Modifier.fillMaxWidth())
  if(coding) Text(L("Code is never executed automatically. Review before running.","कोड अपने आप नहीं चलेगा। चलाने से पहले जाँचें।","কোড নিজে চলবে না। চালানোর আগে যাচাই করুন।"),Modifier.padding(horizontal=16.dp),style=MaterialTheme.typography.bodySmall)
  LazyColumn(state=listState,modifier=Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   if(saved.messages.isEmpty()) item {
    Section(if(coding) "YESMIN · Coding" else "Hello · नमस्ते · হ্যালো") {
     Text(L("I’m YESMIN, your AI assistant—not a human. Type or tap the microphone. Cloud AI needs Firebase setup and your consent.","मैं YESMIN, आपकी AI सहायक हूँ—इंसान नहीं। लिखें या माइक दबाएँ। Cloud AI के लिए Firebase और आपकी अनुमति चाहिए।","আমি YESMIN, আপনার AI সহকারী—মানুষ নই। লিখুন বা মাইক চাপুন। Cloud AI-এর জন্য Firebase ও সম্মতি প্রয়োজন।"))
     Text(if(coding) "Try: Write a simple HTML calculator with explanation." else "Try: Chrome kholo · YouTube par relaxing music search karo")
    }
   }
   items(saved.messages,key={it.id}) { MessageBubble(it,vm::speak,vm::toggleFavouriteCommand) }
  }
  if(partial.isNotBlank() && listening) Text(partial,Modifier.padding(horizontal=16.dp),color=MaterialTheme.colorScheme.secondary)
  if(busy || speaking) TextButton(onClick={vm.cancelRequest()}) { Text(stringResource(R.string.stop)) }
  Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   FilledTonalIconButton(onClick={ if(listening) vm.speech.stop() else microphoneDisclosure=true },enabled=saved.settings.voiceEnabled && !busy) {
    Icon(if(listening) Icons.Default.Stop else Icons.Default.Mic,contentDescription=stringResource(if(listening) R.string.stop else R.string.listen))
   }
   OutlinedTextField(value=input,onValueChange={input=it.take(12000)},modifier=Modifier.weight(1f),placeholder={Text(stringResource(R.string.input))},maxLines=5)
   FilledIconButton(onClick={ val text=input; input=""; vm.send(text,coding) },enabled=input.isNotBlank() && !busy) { Icon(Icons.Default.Send,contentDescription=stringResource(R.string.send)) }
  }
 }
 if(microphoneDisclosure) AlertDialog(onDismissRequest={microphoneDisclosure=false},title={Text(L("Use microphone","माइक्रोफ़ोन का उपयोग","মাইক্রোফোন ব্যবহার"))},text={Text("YESMIN will listen only while this screen is in the foreground, until you stop or recognition ends. Android's selected speech recognition provider may process your audio on its servers. Recognized text is submitted like a chat message; natural-language AI requests require separate cloud consent. No background listening or recording is stored by YESMIN.")},confirmButton={TextButton(onClick={microphoneDisclosure=false;permission.launch(Manifest.permission.RECORD_AUDIO)}){Text("Continue")}},dismissButton={TextButton(onClick={microphoneDisclosure=false}){Text("Use text instead")}})
 if(clearConfirm) AlertDialog(onDismissRequest={clearConfirm=false},title={Text(L("Clear conversation?","बातचीत मिटाएँ","কথোপকথন মুছুন"))},text={Text("Deletes local chat, command and app history. Notes and approved preferences stay. Cloud-provider retention is controlled separately.")},confirmButton={TextButton(onClick={vm.clearHistory();clearConfirm=false}) {Text("Clear")}},dismissButton={TextButton(onClick={clearConfirm=false}){Text("Cancel")}})
}
