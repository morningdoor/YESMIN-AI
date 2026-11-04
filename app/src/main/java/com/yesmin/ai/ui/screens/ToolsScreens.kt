package com.yesmin.ai.ui.screens
import android.Manifest
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yesmin.ai.data.AssistantViewModel
import com.yesmin.ai.ui.components.*
import com.yesmin.ai.utils.Calculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
@Composable fun ToolsScreen(vm:AssistantViewModel,navigate:(String)->Unit) {
 val data by vm.state.collectAsStateWithLifecycle()
 val context=LocalContext.current
 var note by rememberSaveable { mutableStateOf("") }
 var expression by rememberSaveable { mutableStateOf("") }
 var result by rememberSaveable { mutableStateOf("") }
 var reminderText by rememberSaveable { mutableStateOf("") }
 var reminderDate by rememberSaveable { mutableStateOf(SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(Date(System.currentTimeMillis()+3600000))) }
 var deleteNoteId by remember { mutableStateOf<Long?>(null) }
 var deleteReminderId by remember { mutableStateOf<Int?>(null) }
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if(!it)vm.notifyError("Notification denied. You can keep notes, but reminders cannot notify you until enabled.") }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  Section(L("Calculator","कैलकुलेटर","ক্যালকুলেটর")) {
   OutlinedTextField(expression,{expression=it.take(300)},label={Text("Example: (12 + 3) * 4 / 2")},modifier=Modifier.fillMaxWidth())
   Button(onClick={result=runCatching{Calculator.calculate(expression)}.getOrElse{it.message ?: "Invalid expression"}}){Text(L("Calculate","गणना","হিসাব করুন"))}
   if(result.isNotEmpty()) Text(result,style=MaterialTheme.typography.headlineSmall)
   Text("Offline arithmetic: + − × ÷ and parentheses. No code execution.",style=MaterialTheme.typography.bodySmall)
  }
  Section(L("Notes & explicit clipboard paste","नोट्स और क्लिपबोर्ड","নোট ও ক্লিপবোর্ড")) {
   OutlinedTextField(note,{note=it.take(10000)},label={Text("Private local note")},modifier=Modifier.fillMaxWidth(),minLines=3)
   Row {
    TextButton(onClick={
     val clip=context.getSystemService(ClipboardManager::class.java).primaryClip
     note=if(clip!=null && clip.itemCount>0) clip.getItemAt(0).coerceToText(context).toString().take(10000) else ""
    }){Text("Paste clipboard")}
    Button(onClick={vm.addNote(note);note=""},enabled=note.isNotBlank()){Text("Save note")}
   }
   Text("Paste is local only. To ask Gemini about it, explicitly copy your chosen text into AI Chat and send it.",style=MaterialTheme.typography.bodySmall)
   data.notes.reversed().forEach { n ->
    HorizontalDivider(); Text(n.text)
    Row {TextButton(onClick={copyText(context,n.text)}){Text("Copy")};TextButton(onClick={shareText(context,n.text)}){Text("Share")};TextButton(onClick={deleteNoteId=n.id}){Text("Delete")}}
   }
  }
  Section(L("Reminders","रिमाइंडर","স্মরণিকা")) {
   Text("Local inexact notification reminders. Delivery may be delayed by Doze, battery restrictions, force-stop or notification settings; not suitable for critical alarms.")
   if(Build.VERSION.SDK_INT>=33) OutlinedButton(onClick={permission.launch(Manifest.permission.POST_NOTIFICATIONS)}){Text("Enable reminder notifications")}
   OutlinedTextField(reminderText,{reminderText=it.take(500)},label={Text("Reminder text")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(reminderDate,{reminderDate=it.take(20)},label={Text("Local time · yyyy-MM-dd HH:mm")},modifier=Modifier.fillMaxWidth(),singleLine=true)
   Button(onClick={
    if(!NotificationManagerCompat.from(context).areNotificationsEnabled()) vm.notifyError("Enable YESMIN notifications in Android App settings before adding a reminder.")
    else runCatching{vm.addReminder(reminderText,reminderDate)}.onSuccess{reminderText=""}.onFailure{vm.notifyError(it.message ?: "Unable to schedule reminder")}
   }){Text("Add reminder")}
   data.reminders.forEach { r ->
    HorizontalDivider(); Text(r.text)
    Text(SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(Date(r.at)) + if(r.at<System.currentTimeMillis()) " · elapsed" else "",style=MaterialTheme.typography.labelMedium)
    TextButton(onClick={deleteReminderId=r.id}){Text("Cancel & remove")}
   }
  }
  Section(L("AI tools","AI टूल्स","AI টুলস")) {
   Text("Translation, general knowledge, maths explanations, writing and studying use Gemini in AI Chat. Review important answers.")
   listOf("Translate between Hindi, Bangla and English","Help me study and make a quiz","Help me improve a paragraph","Explain a maths problem step by step").forEach { prompt ->
    OutlinedButton(onClick={navigate("chat");vm.send(prompt)}){Text(prompt)}
   }
   OutlinedButton(onClick={navigate("chat");vm.send("Summarize this conversation. Do not perform any device actions.")}){Text("Summarize recent chat with Gemini")}
  }
 }
 if(deleteNoteId!=null) AlertDialog(onDismissRequest={deleteNoteId=null},title={Text("Delete local note")},text={Text("This cannot be undone.")},confirmButton={TextButton(onClick={data.notes.firstOrNull{it.id==deleteNoteId}?.let(vm::deleteNote);deleteNoteId=null}){Text("Delete")}},dismissButton={TextButton(onClick={deleteNoteId=null}){Text("Cancel")}})
 if(deleteReminderId!=null) AlertDialog(onDismissRequest={deleteReminderId=null},title={Text("Cancel reminder")},confirmButton={TextButton(onClick={data.reminders.firstOrNull{it.id==deleteReminderId}?.let(vm::deleteReminder);deleteReminderId=null}){Text("Cancel reminder")}},dismissButton={TextButton(onClick={deleteReminderId=null}){Text("Keep")}})
}
@Composable fun HistoryScreen(vm:AssistantViewModel) {
 val data by vm.state.collectAsStateWithLifecycle()
 var phrase by rememberSaveable { mutableStateOf("") }
 var command by rememberSaveable { mutableStateOf("") }
 var clear by remember { mutableStateOf(false) }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  Section(L("Favourite commands","पसंदीदा कमांड","প্রিয় কমান্ড")) {
   if(data.favouriteCommands.isEmpty()) Text("Tap the star beside a sent chat message to save a favourite command.")
   data.favouriteCommands.forEach { cmd ->
    Text(cmd)
    Row {TextButton(onClick={vm.send(cmd)}){Text("Run / preview")};TextButton(onClick={vm.toggleFavouriteCommand(cmd)}){Text("Remove")}}
   }
  }
  Section(L("Custom command shortcuts","कस्टम शॉर्टकट","নিজস্ব শর্টকাট")) {
   OutlinedTextField(phrase,{phrase=it.take(120)},label={Text("Phrase, e.g. focus time")},modifier=Modifier.fillMaxWidth())
   OutlinedTextField(command,{command=it.take(1000)},label={Text("Command, e.g. YouTube par piano search karo")},modifier=Modifier.fillMaxWidth())
   Button(onClick={runCatching{vm.addShortcut(phrase,command)}.onSuccess{phrase="";command=""}.onFailure{vm.notifyError(it.message ?: "Invalid shortcut")}}){Text("Save shortcut")}
   Text("Shortcuts expand once only. They never run automatically; external actions still need confirmation. Gesture shortcuts are rejected.",style=MaterialTheme.typography.bodySmall)
   data.shortcuts.forEach { s ->
    Text("${s.phrase} → ${s.command}")
    Row { TextButton(onClick={vm.send(s.phrase)}){Text("Run / preview")}; TextButton(onClick={vm.removeShortcut(s)}){Text("Remove")} }
   }
  }
  Section(L("Command history","कमांड इतिहास","কমান্ড ইতিহাস")) {
   Text(if(data.settings.saveHistory) "Stored locally until cleared (latest 100)." else "Session only; history saving is disabled.")
   TextButton(onClick={clear=true}){Text("Clear chat, commands & recent apps")}
   data.commands.forEach { c ->
    HorizontalDivider(); Text(c.text,style=MaterialTheme.typography.titleSmall); Text(c.result)
    Text(SimpleDateFormat("MMM d, HH:mm",Locale.getDefault()).format(Date(c.time)),style=MaterialTheme.typography.labelSmall)
    Row {TextButton(onClick={vm.send(c.text)}){Text("Repeat / preview")};TextButton(onClick={vm.toggleFavouriteCommand(c.text)}){Text("Favourite")}}
   }
  }
 }
 if(clear) AlertDialog(onDismissRequest={clear=false},title={Text("Clear local history")},text={Text("Chat, command history and recently opened apps will be removed. Notes and reminders remain.")},confirmButton={TextButton(onClick={vm.clearHistory();clear=false}){Text("Clear")}},dismissButton={TextButton(onClick={clear=false}){Text("Cancel")}})
}
