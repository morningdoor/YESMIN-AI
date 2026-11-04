package com.yesmin.ai.reminders
import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.yesmin.ai.MainActivity
import com.yesmin.ai.R
import com.yesmin.ai.data.LocalStore
import com.yesmin.ai.data.Reminder
class ReminderScheduler(private val context: Context) {
 private fun pending(r: Reminder) = PendingIntent.getBroadcast(context, r.id,
  Intent(context, ReminderReceiver::class.java).putExtra("id", r.id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
 fun schedule(r: Reminder) {
  require(r.at > System.currentTimeMillis()) { "Reminder must be in the future" }
  context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.at, pending(r))
 }
 fun cancel(r: Reminder) { context.getSystemService(AlarmManager::class.java).cancel(pending(r)) }
}
class ReminderReceiver : BroadcastReceiver() {
 override fun onReceive(context: Context, intent: Intent) {
  val store=LocalStore(context)
  val r=store.state.value.reminders.firstOrNull { it.id==intent.getIntExtra("id",-1) } ?: return
  val manager=context.getSystemService(NotificationManager::class.java)
  manager.createNotificationChannel(NotificationChannel("reminders", "YESMIN reminders", NotificationManager.IMPORTANCE_DEFAULT))
  val allowed=Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED
  if(allowed) {
   val open=PendingIntent.getActivity(context, r.id, Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
   manager.notify(r.id,NotificationCompat.Builder(context,"reminders").setSmallIcon(R.drawable.ic_yesmin)
    .setContentTitle("YESMIN reminder").setContentText(r.text).setStyle(NotificationCompat.BigTextStyle().bigText(r.text))
    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setContentIntent(open).setAutoCancel(true).build())
  }
  // Keep overdue entries in the list until the user removes them; do not silently lose blocked reminders.
 }
}
class BootReceiver : BroadcastReceiver() {
 override fun onReceive(context: Context, intent: Intent) {
  if(intent.action != Intent.ACTION_BOOT_COMPLETED) return
  val now=System.currentTimeMillis()
  LocalStore(context).state.value.reminders.filter { it.at>now }.forEach { runCatching { ReminderScheduler(context).schedule(it) } }
 }
}
