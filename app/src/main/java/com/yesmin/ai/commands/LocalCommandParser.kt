package com.yesmin.ai.commands
object LocalCommandParser {
 private fun clean(s: String) = s.trim().replace(Regex("^(yesmin ai|yesmin|येसमिन|ইয়েসমিন)(?:[,।! ]+|$)", RegexOption.IGNORE_CASE), "").trim().trimEnd('.', '!', '?', '।')
 fun normalize(s: String) = clean(s).lowercase()
 fun gesture(raw: String): GestureRequest? {
  val s = normalize(raw)
  val kind = when (s) {
   "swipe up", "upar swipe karo", "ऊपर स्वाइप करो", "উপরে সোয়াইপ করো" -> "UP"
   "swipe down", "neeche swipe karo", "नीचे स्वाइप करो", "নিচে সোয়াইপ করো" -> "DOWN"
   "go back", "back jao", "पीछे जाओ", "পিছনে যাও" -> "BACK"
   "go home", "home jao", "होम जाओ", "হোমে যাও" -> "HOME"
   "open recent apps", "recent apps", "रीसेंट ऐप्स", "সাম্প্রতিক অ্যাপ" -> "RECENTS"
   else -> null
  }
  if (kind != null) return GestureRequest(kind)
  // Tap is a strict local grammar. Coordinates are screen pixels, not AI-selected targets.
  val m = Regex("^tap\\s+(\\d{1,5})\\s*[, ]\\s*(\\d{1,5})$").matchEntire(s) ?: return null
  return GestureRequest("TAP", m.groupValues[1].toFloat(), m.groupValues[2].toFloat())
 }
 fun parse(raw: String): BrainReply? {
  val s = normalize(raw)
  val sites = mapOf("google" to "https://www.google.com", "wikipedia" to "https://www.wikipedia.org", "youtube" to "https://www.youtube.com")
  if(s in setOf("settings mein jao", "settings me jao", "सेटिंग्स में जाओ", "সেটিংসে যাও"))
   return BrainReply("Ready to open Android Settings.", listOf(ProposedAction("OPEN_APP", "settings")))
  val inChrome = Regex("^chrome (?:mein|me|में) (.+?) (?:open karo|kholo|खोलो)$",RegexOption.IGNORE_CASE).matchEntire(clean(raw))
  val websiteCommand = Regex("^(.+?) website (?:par jao|open karo|kholo)$").matchEntire(s)
  val siteName = inChrome?.groupValues?.get(1) ?: websiteCommand?.groupValues?.get(1)
  if(siteName != null) {
   val target = sites[siteName.lowercase()] ?: if(siteName.startsWith("https://")) siteName else "https://$siteName"
   if(ActionValidator.validUrl(target)) return BrainReply("Ready to open this website.",listOf(ProposedAction("OPEN_URL",target)))
   return null
  }
  // Deterministic collapse: a browser/app open plus search becomes one search intent.
  val multi = Regex("^(?:chrome|youtube) (?:kholo|open karo|open) (?:aur|and) (.+)$").matchEntire(s)
  if (multi != null) {
   val rest = multi.groupValues[1]
   if(!rest.endsWith(" search karo") && !rest.endsWith(" search")) return null
   val query = rest.removePrefix("google par ").removePrefix("youtube par ").removeSuffix(" search karo").removeSuffix(" search").trim()
   if (query.isNotBlank()) return BrainReply("Ready to search.", listOf(ProposedAction(if(s.startsWith("youtube")) "YOUTUBE_SEARCH" else "WEB_SEARCH", query)))
  }
  val yt = Regex("^(?:youtube(?: par| pe)? |play |ইউটিউবে |यूट्यूब पर )(.+?)(?: search karo| search| खोजो| খোঁজো| on youtube)$").matchEntire(s)
  if (yt != null) return BrainReply("YouTube search results will open; select a result to play.", listOf(ProposedAction("YOUTUBE_SEARCH", yt.groupValues[1].trim())))
  val web = Regex("^(?:search |google par |google pe |গুগলে |गूगल पर )(.+?)(?: search karo| search| खोजो| খোঁজো)?$").matchEntire(s)
  if (web != null) return BrainReply("Ready to open search results.", listOf(ProposedAction("WEB_SEARCH", web.groupValues[1].trim())))
  val url = Regex("^(?:open )?((?:https://)?[a-z0-9.-]+\\.[a-z]{2,}(?:/[^ ]*)?)(?: in chrome| kholo| open karo)?$",RegexOption.IGNORE_CASE).matchEntire(clean(raw))
  if (url != null) {
   val value = url.groupValues[1].let { if(it.startsWith("https://",true)) "https://"+it.substring(8) else "https://$it" }
   return BrainReply("Ready to open this website.", listOf(ProposedAction("OPEN_URL", value)))
  }
  val app = Regex("^(?:open (.+)|(.+?) (?:kholo|open karo|open|खोलो|খোলো|খুলুন)|(.+?) app open karo)$").matchEntire(s)
  if (app != null) {
   val name = app.groupValues.drop(1).firstOrNull { it.isNotBlank() }?.removeSuffix(" app")?.trim() ?: return null
   if(name.contains(Regex("\\b(?:aur|and|mein)\\b"))) return null
   if (name == "google") return BrainReply("Ready to open Google.", listOf(ProposedAction("OPEN_URL", "https://www.google.com")))
   return BrainReply("Ready to open $name.", listOf(ProposedAction("OPEN_APP", name)))
  }
  return null
 }
}
