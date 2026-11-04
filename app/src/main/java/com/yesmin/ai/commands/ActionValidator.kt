package com.yesmin.ai.commands
import java.net.URI
object ActionValidator {
 val allowed = setOf("OPEN_APP", "OPEN_URL", "WEB_SEARCH", "YOUTUBE_SEARCH")
 fun validUrl(value: String): Boolean = runCatching {
  val u = URI(value)
  val host = u.host.orEmpty()
  val h = host.lowercase()
  u.scheme == "https" && host.contains('.') && u.rawUserInfo == null &&
   (u.port == -1 || u.port == 443) && !h.endsWith(".local") && !h.endsWith(".internal") &&
   h != "localhost" && !h.endsWith(".localhost") && !h.endsWith(".lan") && !h.endsWith(".home.arpa") &&
   ':' !in host && !h.matches(Regex("[0-9.]+")) &&
   value.none { it.isWhitespace() || it.isISOControl() }
 }.getOrDefault(false)
 fun validate(actions: List<ProposedAction>): Result<List<ProposedAction>> = runCatching {
  require(actions.size <= 3) { "At most three actions are supported. Split this request." }
  actions.forEach {
   require(it.type in allowed) { "Unsupported action rejected: ${it.type.take(40)}" }
   require(it.argument.isNotBlank() && it.argument.length <= 1000 && it.argument.none(Char::isISOControl)) { "Invalid action argument" }
   if (it.type == "OPEN_URL") require(validUrl(it.argument)) { "Only public HTTPS websites are supported; no credentials or local addresses." }
   if (it.type == "OPEN_APP") require(it.argument.length <= 120) { "App name too long" }
  }
  actions.toList()
 }
}
