package com.yesmin.ai.accessibility
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import com.yesmin.ai.commands.GestureRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
class YesminAccessibilityService : AccessibilityService() {
 companion object { @Volatile var instance: YesminAccessibilityService? = null; private set }
 override fun onServiceConnected() { instance = this }
 // No node traversal, screen text, event logging, or authentication-field access.
 override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
 override fun onInterrupt() {}
 override fun onDestroy() { if(instance === this) instance = null; super.onDestroy() }
 suspend fun perform(request: GestureRequest, consentAt: Long): Boolean {
  // One explicit request only; stale confirmation cannot be reused for automation.
  if(android.os.SystemClock.elapsedRealtime() - consentAt !in 0..10000) return false
  when(request.kind) {
   "BACK" -> return performGlobalAction(GLOBAL_ACTION_BACK)
   "HOME" -> return performGlobalAction(GLOBAL_ACTION_HOME)
   "RECENTS" -> return performGlobalAction(GLOBAL_ACTION_RECENTS)
  }
  val width=resources.displayMetrics.widthPixels.toFloat(); val height=resources.displayMetrics.heightPixels.toFloat()
  val path=Path()
  when(request.kind) {
   "UP" -> { path.moveTo(width/2, height*0.75f); path.lineTo(width/2, height*0.3f) }
   "DOWN" -> { path.moveTo(width/2, height*0.3f); path.lineTo(width/2, height*0.75f) }
   "TAP" -> {
    if(request.x !in 0f..<width || request.y !in 0f..<height) return false
    path.moveTo(request.x, request.y)
   }
   else -> return false
  }
  val gesture=GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path,0,if(request.kind=="TAP") 80 else 350)).build()
  return suspendCancellableCoroutine { continuation ->
   val accepted = dispatchGesture(gesture, object : GestureResultCallback() {
    override fun onCompleted(gestureDescription: GestureDescription?) { if(continuation.isActive) continuation.resume(true) }
    override fun onCancelled(gestureDescription: GestureDescription?) { if(continuation.isActive) continuation.resume(false) }
   }, null)
   if(!accepted && continuation.isActive) continuation.resume(false)
  }
 }
}
