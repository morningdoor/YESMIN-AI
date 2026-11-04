package com.yesmin.ai.commands
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.yesmin.ai.apps.AppRepository
class CommandRouter(private val context: Context, private val apps: AppRepository) {
 fun execute(action: ProposedAction): String {
  ActionValidator.validate(listOf(action)).getOrThrow()
  return when(action.type) {
   "OPEN_APP" -> apps.openNamed(action.argument)
   "OPEN_URL" -> openUrl(action.argument, true)
   "WEB_SEARCH" -> openUrl("https://www.google.com/search?q=" + Uri.encode(action.argument), true)
   "YOUTUBE_SEARCH" -> {
    val url = "https://www.youtube.com/results?search_query=" + Uri.encode(action.argument)
    openUrl(url, false, "com.google.android.youtube")
   }
   else -> error("Unsupported action")
  }
 }
 private fun openUrl(url: String, chrome: Boolean, preferred: String? = null): String {
  require(ActionValidator.validUrl(url)) { "Unsafe website URL" }
  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  val pkg = preferred ?: if(chrome) "com.android.chrome" else null
  if(pkg != null) {
   try { context.startActivity(Intent(intent).setPackage(pkg)); return "Opened requested app/website. Select any search result yourself." }
   catch (_: android.content.ActivityNotFoundException) { /* Use a browser if the preferred app is unavailable. */ }
  }
  try { context.startActivity(intent) }
  catch (_: android.content.ActivityNotFoundException) { error("No compatible browser is installed. Install a browser and try again.") }
  return "Opened with an available handler/browser."
 }
}
