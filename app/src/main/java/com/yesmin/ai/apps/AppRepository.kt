package com.yesmin.ai.apps
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.Settings
import com.yesmin.ai.data.LocalStore
data class InstalledApp(val name: String, val packageName: String, val activityName: String, val icon: Drawable)
class AppRepository(private val context: Context, private val store: LocalStore) {
 @Volatile var apps: List<InstalledApp> = emptyList(); private set
 @Suppress("DEPRECATION") fun refresh(): List<InstalledApp> {
  apps = context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
   .map { InstalledApp(it.loadLabel(context.packageManager).toString(), it.activityInfo.packageName, it.activityInfo.name, it.loadIcon(context.packageManager)) }
   .filter { it.packageName != context.packageName }.distinctBy { it.packageName }.sortedBy { it.name.lowercase() }
  return apps
 }
 fun find(name: String): List<InstalledApp> {
  val rawKey = name.trim().lowercase()
  val key = when(rawKey) {
   "क्रोम", "ক্রোম" -> "chrome"
   "यूट्यूब", "ইউটিউব" -> "youtube"
   "क्लॉड", "ক্লড" -> "claude"
   "प्ले स्टोर", "প্লে স্টোর" -> "play store"
   else -> rawKey
  }
  val alias = mapOf("chrome" to "com.android.chrome", "youtube" to "com.google.android.youtube", "claude" to "com.anthropic.claude", "play store" to "com.android.vending")
  alias[key]?.let { p -> apps.firstOrNull { it.packageName == p }?.let { return listOf(it) } }
  val exact = apps.filter { it.name.equals(name, true) || it.packageName == name }
  return exact.ifEmpty { apps.filter { it.name.contains(name, true) } }
 }
 fun launch(app: InstalledApp): String {
  context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
   .setComponent(ComponentName(app.packageName, app.activityName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  store.update { it.copy(recentApps = (listOf(app.packageName) + it.recentApps).distinct().take(12)) }
  return "Opened ${app.name}."
 }
 fun openNamed(name: String): String {
  if (name.lowercase() in setOf("settings", "सेटिंग्स", "সেটিংস")) {
   context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return "Opened Android Settings."
  }
  val special = when(name.lowercase()) {
   "camera", "कैमरा", "ক্যামেরা" -> Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
   "gallery", "गैलरी", "গ্যালারি" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY)
   "files", "फाइल्स", "ফাইলস" -> Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE)
   "calculator", "कैलकुलेटर", "ক্যালকুলেটর" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)
   else -> null
  }
  if (special != null) {
   try { context.startActivity(special.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return "Opened $name." }
   catch (_: android.content.ActivityNotFoundException) { /* Fall through to visible launcher apps. */ }
  }
  val matches = find(name)
  require(matches.isNotEmpty()) { "$name is not installed or has no visible launcher. Try the App Launcher." }
  require(matches.size == 1) { "More than one matching app. Select the exact app in App Launcher." }
  return launch(matches.single())
 }
}
