# YESMIN AI — native Android v1

YESMIN is a female-personality AI assistant, not a human. Native Kotlin, Jetpack Compose, Material 3 and MVVM. This is source for a real Android app, not a website or UI-only demo.

## Start here (phone + Termux)

Read **TERMUX_BUILD.md** first. The supported phone-managed build uses GitHub Actions on an x86-64 Linux runner, triggered from Termux. You manage source and install the APK from the phone. It does NOT pretend that Google's standard Linux SDK tools run natively on ARM/Bionic Termux.

1. Extract this project under Termux `$HOME`, not shared storage.
2. Follow **FIREBASE_SETUP.md** for cloud AI. Without Firebase client configuration the APK still builds and local tools work, but Gemini calls are unavailable.
3. Push to a private GitHub repo, set the optional Firebase config secret, and trigger `.github/workflows/android.yml`.
4. Download and install the debug APK. Register its Firebase App Check debug token before using Gemini with enforcement enabled.
5. Review **Privacy & Permissions** in the app and explicitly consent to cloud processing.

**No Gemini secret API key is included or required in client code.** Firebase AI Logic handles the selected backend; App Check must be configured AND enforced in the Firebase console. This ZIP cannot contain your Firebase project configuration or credentials because none were provided.

## Implemented

- Home, AI Chat, Voice Assistant, App Launcher, Coding Assistant, Command History, Settings, Gemini Configuration and Privacy screens; additional Notes & Tools screen.
- AMOLED dark/purple/cyan Compose theme, light/system themes, lightweight status orb, listening/processing/speaking states, responsive text input and message bubbles.
- Firebase AI Logic Gemini requests with structured JSON output schema, strict JSON decoding, native action allowlist, bounded context and explicitly shared preferences.
- Default model `gemini-3.8-flash`; selectable `gemini-3.5-flash-lite` and custom validated Gemini model ID. Availability is controlled by your project/provider; model IDs can be changed without rebuilding.
- Gemini connection test, model save/reset, local disconnect, session request counters, error handling, compiled project ID display, non-secret settings sharing. Counters are NOT token/billing estimates.
- Tap-to-listen Android SpeechRecognizer, partial text, stop, errors and runtime microphone permission; Android TTS voice list/selection, preview, rate, automatic/manual reading and stop. Speech stops when the app leaves the foreground. No wake word or background microphone.
- Visible launcher-app inventory with icons, text/voice search, favourites and recent apps. Narrow manifest launcher-intent query; no QUERY_ALL_PACKAGES.
- Android intents for apps, camera, gallery handler, file picker, Android settings, calculator handler, browser/public HTTPS URLs, Google web search and encoded YouTube search results with browser fallback. If a requested handler is missing, the app reports it. No guaranteed YouTube autoplay.
- Deterministic local common Hinglish/Hindi/Bangla/English command forms, plus Gemini interpretation for other natural commands and multi-part requests.
- Explicit preview before each external action. A sequence has at most 3 allowlisted actions and runs ONE action per confirmation; return to YESMIN to confirm subsequent steps. No generic autonomous automation.
- Optional accessibility gestures: exact local command grammar only; no Gemini action can invoke a gesture. One confirmation, cancellable 3-second countdown, minimize to the previous screen, one gesture, expiration and cancellation. No screen-text retrieval, node traversal, overlays or authentication-field reading.
- Coding chat, code blocks, selection, copy/share whole answer or code only, code explanation/debugging/formatting via Gemini. Never executes generated code or scripts. Open your installed coding app via App Launcher or an explicit app-open command.
- General knowledge, translation, maths explanations, writing, study assistance and summaries via Gemini chat. Separate offline bounded arithmetic calculator.
- Local notes, explicit clipboard paste (does not send automatically), inexact local reminder notifications with reboot rescheduling, favourites, custom non-recursive command shortcuts, chat/command/app history controls.
- User-approved preferences only. History persistence OFF by default. Local reset cancels reminders, stops requests and disables this accessibility service.
- English/Hindi/Bengali core navigation, voice language and assistant language selection. Some detailed technical/settings/privacy text remains English in this first version; this is not a fully translated production localization.

## Prerequisites and limits

Android 8.0/API 26 or newer. Cloud AI needs internet, your configured Firebase project, allowed model, cloud consent and valid App Check attestation. Speech recognition and voice quality/languages depend on the phone's installed engines. Android does not reliably expose gender metadata for TTS voices: YESMIN prefers an available female-labelled voice, but you must audition/select the desired voice. No female voice is bundled.

Accessibility must be manually enabled in Android AND approved in the app. YESMIN is NOT declared as a disability accessibility tool. Play distribution requires your policy/declaration review. For some sideloads Android's restricted-settings protection may require manual user approval; do not bypass it. Protected screens may reject gestures. Since no screen data is read, YESMIN cannot identify sensitive buttons: do not request screen gestures on payments/authentication/messages/deletion. All targets are user-controlled.

Reminders are **inexact**, not alarm-clock-grade. Android Doze, force-stop and notification/battery restrictions can delay/prevent delivery. Notification permission and channel settings must allow them. No exact-alarm permission is requested.

Local app-private storage is not an additional encrypted secrets vault. Backups are disabled. Do not save highly sensitive notes. Clearing local data does not delete cloud-provider-side records. No telemetry or automatic error report uploads are implemented. Use the local error message plus TESTING.md for troubleshooting, not raw credential/prompt logs.

No background listening, automatic message sending, purchases, file deletion, sensitive-setting modification, arbitrary shell execution or screen-reading AI is implemented. Future features must preserve these boundaries.

## Architecture

```
Compose screens / explicit touch or SpeechRecognizer result
                  |
           AssistantViewModel (MVVM + coroutine jobs)
                  |
       local exact parser OR GeminiRepository
                  |           |
                  |      Firebase AI Logic -> Gemini
                  |      App Check + JSON response schema
                  v
          strict ActionValidator
                  |
          per-action preview/consent
                  |
          CommandRouter -> Android Intent

Exact current-user gesture text (NEVER model output)
 -> app consent gate + manually enabled service
 -> gesture preview -> cancellable countdown
 -> one AccessibilityService gesture

LocalStore -> app-private SharedPreferences JSON + StateFlow
ReminderScheduler -> inexact AlarmManager -> private notification
```

App/Activity context does not leak into the ViewModel: long-lived components use Application context; the Activity supplies only the one-shot minimize callback. Coroutines are lifecycle-owned. Recognition/TTS are stopped on foreground loss and released on ViewModel destruction. AI requests are cancelable and bounded by timeouts.

## Pinned dependency versions

| Component | Version |
|---|---|
| JDK required/recommended | Full JDK 17 |
| Gradle wrapper | 8.11.1 |
| Android Gradle Plugin | 8.9.2 |
| Kotlin Android / Compose compiler / serialization plugins | 2.1.20 |
| compileSdk / targetSdk / minSdk | 35 / 35 / 26 |
| Android build-tools | 35.0.0 |
| Compose BOM | 2025.04.01 |
| Activity Compose | 1.10.1 |
| AndroidX Core | 1.16.0 |
| Lifecycle runtime/ViewModel Compose | 2.9.0 |
| Kotlin Coroutines Android | 1.10.2 |
| Kotlin Serialization JSON | 1.8.0 |
| Firebase BOM | 34.7.0 |
| Firebase AI Logic (resolved by BOM) | 17.7.0 |
| Google Services Gradle plugin | 4.4.2 |
| JUnit | 4.13.2 |

These are intentionally pinned stable artifacts, not a claim to be the newest possible toolchain. The selected versions are checked by the actual Gradle build; see BUILD_VERIFICATION.md. Re-check current Google Play target-API requirements before publishing. This v1 is for personal sideload/testing, not a claim of store approval.

## Code areas

`ai/` cloud brain, `commands/` deterministic routing/security, `voice/` recognition/TTS, `apps/` launcher inventory, `accessibility/` narrowly scoped gestures, `coding/` code extraction, `data/` state/persistence/ViewModel, `reminders/` alarms and notifications, `utils/` arithmetic, `ui/` Compose screens/navigation/theme/components. Debug/release source sets install distinct App Check providers. Main manifest contains no broad storage, contacts, SMS, call or package-list permission.

See PROJECT_TREE.txt for the exact complete file tree, DEVELOPMENT_STAGES.md for the implementation map and TESTING.md for automated/manual checks.

## Official references

- Firebase setup: https://firebase.google.com/docs/ai-logic/get-started
- App Check: https://firebase.google.com/docs/ai-logic/app-check
- Supported models: https://firebase.google.com/docs/ai-logic/models
- Android Gradle Plugin 8.9 requirements: https://developer.android.com/build/releases/past-releases/agp-8-9-0-release-notes
- Android Linux host requirements: https://developer.android.com/studio/install
- Termux GitHub CLI package: https://github.com/termux/termux-packages/blob/master/packages/gh/build.sh
