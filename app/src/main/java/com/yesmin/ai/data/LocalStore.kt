package com.yesmin.ai.data
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
class LocalStore(context: Context) {
 private val prefs = context.getSharedPreferences("yesmin_local_v1", Context.MODE_PRIVATE)
 private val json = Json { ignoreUnknownKeys = true }
 private val mutable = MutableStateFlow(runCatching {
  json.decodeFromString<SavedState>(prefs.getString("state", "{}") ?: "{}")
 }.getOrDefault(SavedState()))
 val state = mutable.asStateFlow()
 @Synchronized fun update(block: (SavedState) -> SavedState) {
  val next = block(mutable.value)
  mutable.value = next
  // History is kept in memory for the current session unless explicitly opted in.
  val disk = if (next.settings.saveHistory) next else next.copy(messages = emptyList(), commands = emptyList(), recentApps = emptyList())
  prefs.edit().putString("state", json.encodeToString(disk)).apply()
 }
 fun settings(block: (UserSettings) -> UserSettings) = update { it.copy(settings = block(it.settings)) }
 fun addMessage(role: String, text: String) = update { it.copy(messages = (it.messages + Message(role = role, text = text)).takeLast(200)) }
 fun log(text: String, result: String) = update { it.copy(commands = (listOf(CommandEntry(text, result)) + it.commands).take(100)) }
 fun clearHistory() = update { it.copy(messages = emptyList(), commands = emptyList(), recentApps = emptyList()) }
 fun reset() = update { SavedState() }
}
