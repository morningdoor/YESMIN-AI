package com.yesmin.ai
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.yesmin.ai.data.AssistantViewModel
import com.yesmin.ai.ui.navigation.YesminApp
class MainActivity : ComponentActivity() {
 private val model: AssistantViewModel by viewModels()
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState); enableEdgeToEdge()
  setContent { YesminApp(model, minimize = { moveTaskToBack(true) }) }
 }
 override fun onStart() { super.onStart(); model.onForeground(true) }
 override fun onStop() { model.onForeground(false); super.onStop() }
}
