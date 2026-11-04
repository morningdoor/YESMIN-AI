package com.yesmin.ai.data
import kotlinx.serialization.Serializable
@Serializable data class Message(val id: Long = System.nanoTime(), val role: String, val text: String, val time: Long = System.currentTimeMillis())
@Serializable data class CommandEntry(val text: String, val result: String, val time: Long = System.currentTimeMillis())
@Serializable data class Note(val id: Long = System.currentTimeMillis(), val text: String)
@Serializable data class Reminder(val id: Int, val text: String, val at: Long)
@Serializable data class Shortcut(val phrase: String, val command: String)
@Serializable data class UserSettings(
 val language: String = "en", val theme: String = "dark", val voiceName: String = "",
 val speechRate: Float = 1f, val voiceEnabled: Boolean = true, val autoSpeak: Boolean = true,
 val saveHistory: Boolean = false, val useContext: Boolean = true,
 val model: String = "gemini-3.8-flash", val aiEnabled: Boolean = true,
 val preferences: String = "", val sharePreferences: Boolean = false,
 val cloudConsent: Boolean = false, val screenEnabled: Boolean = false
)
@Serializable data class SavedState(
 val settings: UserSettings = UserSettings(), val messages: List<Message> = emptyList(),
 val commands: List<CommandEntry> = emptyList(), val notes: List<Note> = emptyList(),
 val reminders: List<Reminder> = emptyList(), val favourites: Set<String> = emptySet(),
 val recentApps: List<String> = emptyList(), val favouriteCommands: List<String> = emptyList(),
 val shortcuts: List<Shortcut> = emptyList()
)
