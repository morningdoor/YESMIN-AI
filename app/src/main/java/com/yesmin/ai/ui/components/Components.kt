package com.yesmin.ai.ui.components
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yesmin.ai.coding.CodeBlocks
import com.yesmin.ai.data.Message
@Composable fun L(en:String,hi:String,bn:String):String = when(LocalConfiguration.current.locales[0].language) { "hi"->hi; "bn"->bn; else->en }
@Composable fun VoiceOrb(status:String,active:Boolean,size:Int=128) {
 val colors=MaterialTheme.colorScheme
 val pulse=if(active) {
  val t=rememberInfiniteTransition(label="voice pulse")
  t.animateFloat(0.82f,1.08f,infiniteRepeatable(tween(900),RepeatMode.Reverse),label="pulse").value
 } else 0.9f
 Box(Modifier.size(size.dp),contentAlignment=Alignment.Center) {
  Canvas(Modifier.fillMaxSize()) {
   val r=this.size.minDimension/2
   drawCircle(Brush.radialGradient(listOf(colors.primary.copy(alpha=0.28f),colors.secondary.copy(alpha=0.03f))),r*pulse)
   drawCircle(colors.primary.copy(alpha=0.55f),r*0.78f*pulse,style=Stroke(2.dp.toPx()))
   drawCircle(colors.secondary,r*0.63f,style=Stroke(1.dp.toPx()))
   for(i in 0..10) {
    val x=center.x+(i-5)*r/12
    val h=(if(i%3==0) 0.3f else if(i%2==0) 0.5f else 0.18f)*r*pulse
    drawLine(if(i%2==0) colors.primary else colors.secondary,Offset(x,center.y-h/2),Offset(x,center.y+h/2),3.dp.toPx())
   }
  }
 }
 Text(status,style=MaterialTheme.typography.labelLarge,color=colors.secondary)
}
fun copyText(context:Context,text:String) { context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("YESMIN text",text)) }
fun shareText(context:Context,text:String) { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,text),"Share text").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
@Composable fun MessageBubble(message:Message,onSpeak:(String)->Unit,onFavourite:(String)->Unit) {
 val context=LocalContext.current
 val mine=message.role=="user"
 Column(Modifier.fillMaxWidth(),horizontalAlignment=if(mine) Alignment.End else Alignment.Start) {
  Surface(color=if(mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
   shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth(if(mine) 0.91f else 1f)) {
   Column(Modifier.padding(14.dp)) {
    Text(if(mine) L("YOU","आप","আপনি") else "YESMIN",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary,fontWeight=FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    SelectionContainer { Text(message.text,style=MaterialTheme.typography.bodyLarge) }
    Row(horizontalArrangement=Arrangement.spacedBy(2.dp)) {
     IconButton(onClick={ copyText(context,message.text) }) { Icon(Icons.Default.ContentCopy,contentDescription="Copy text") }
     IconButton(onClick={ shareText(context,message.text) }) { Icon(Icons.Default.Share,contentDescription="Share text") }
     if(mine) IconButton(onClick={onFavourite(message.text)}) { Icon(Icons.Default.StarOutline,contentDescription="Favourite command") }
     else IconButton(onClick={onSpeak(message.text)}) { Icon(Icons.Default.VolumeUp,contentDescription="Read aloud") }
    }
    if(!mine) CodeBlocks.extract(message.text).forEach { block ->
     Surface(color=MaterialTheme.colorScheme.background,shape=RoundedCornerShape(12.dp)) {
      Column(Modifier.padding(10.dp)) {
       Text(block.language.ifBlank { "code" },color=MaterialTheme.colorScheme.secondary)
       SelectionContainer { Text(block.code,fontFamily=FontFamily.Monospace,style=MaterialTheme.typography.bodySmall) }
       Row {
        TextButton(onClick={copyText(context,block.code)}) { Text(L("Copy code","कोड कॉपी","কোড কপি")) }
        TextButton(onClick={shareText(context,block.code)}) { Text(L("Share","शेयर","শেয়ার")) }
       }
      }
     }
    }
   }
  }
 }
}
@Composable fun Section(title:String,content:@Composable ColumnScope.()->Unit) {
 Card(Modifier.fillMaxWidth()) {
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Text(title,style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)
   content()
  }
 }
}
@Composable fun Toggle(title:String,checked:Boolean,onChange:(Boolean)->Unit,detail:String="") {
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
  Column(Modifier.weight(1f)) { Text(title); if(detail.isNotBlank()) Text(detail,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
  Switch(checked=checked,onCheckedChange=onChange)
 }
}
