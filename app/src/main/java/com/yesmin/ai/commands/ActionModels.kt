package com.yesmin.ai.commands
import kotlinx.serialization.Serializable
@Serializable data class ProposedAction(val type: String, val argument: String)
@Serializable data class BrainReply(val reply: String, val actions: List<ProposedAction> = emptyList())
data class GestureRequest(val kind: String, val x: Float = 0f, val y: Float = 0f)
