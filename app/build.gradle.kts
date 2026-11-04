plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
 id("org.jetbrains.kotlin.plugin.serialization")
}
// The app can compile without private Firebase project configuration.
if (file("google-services.json").exists()) apply(plugin = "com.google.gms.google-services")
android {
 namespace = "com.yesmin.ai"
 compileSdk = 35
 defaultConfig {
  applicationId = "com.yesmin.ai"
  minSdk = 26
  targetSdk = 35
  versionCode = 1
  versionName = "1.0.0"
  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
 }
 buildFeatures { compose = true; buildConfig = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 buildTypes {
  release { isMinifyEnabled = false; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") }
 }
 packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.04.01"))
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.ui:ui-tooling-preview")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-extended")
 debugImplementation("androidx.compose.ui:ui-tooling")
 implementation("androidx.activity:activity-compose:1.10.1")
 implementation("androidx.core:core-ktx:1.16.0")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
 implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
 implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
 implementation("com.google.firebase:firebase-ai")
 implementation("com.google.firebase:firebase-appcheck-playintegrity")
 debugImplementation("com.google.firebase:firebase-appcheck-debug")
 testImplementation("junit:junit:4.13.2")
}
