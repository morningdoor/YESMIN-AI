package com.yesmin.ai
import android.app.Application
import com.google.firebase.FirebaseApp
import com.yesmin.ai.security.AppCheckInstaller
class YesminApplication : Application() {
 override fun onCreate() {
  super.onCreate()
  if (FirebaseApp.initializeApp(this) != null) AppCheckInstaller.install()
 }
}
