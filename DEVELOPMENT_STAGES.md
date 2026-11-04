# Staged implementation map

This map explains which code implements each requested stage. The source was assembled in logical stage groups and checked using the final Gradle compile/tests/lint gate; it would be inaccurate to claim a physical-device acceptance test after each file was written. Initial compile errors were corrected before packaging. See BUILD_VERIFICATION.md for exact build results and TESTING.md for the manual matrix.

| Stage | Files / implementation |
|---|---|
| 1 Architecture | Root/app Gradle, wrapper, manifest, Models.kt, LocalStore.kt; MVVM + Coroutines/StateFlow; debug/release AppCheck source sets. |
| 2 UI/navigation | MainActivity.kt, ui/theme, ui/components, ui/navigation, Home and all requested destinations with Material 3, core locale resources and animated status orb. |
| 3 Gemini | ai/GeminiRepository.kt, security/AppCheckInstaller.kt, GeminiScreen; Firebase AI Logic, response schema, strict decode, connection test/status/model controls; no raw Gemini client key. |
| 4 Voice | voice/SpeechController.kt, TtsController.kt, ChatScreen; explicit permission, tap/stop/partial/error lifecycle, selected locale, installed voices/rate and safe TTS cleanup. |
| 5 Apps | apps/AppRepository.kt, LauncherScreen; launcher-intent visibility, icons/search/favourites/recents, explicit launch and missing-handler feedback. |
| 6 Browser/YouTube | commands/LocalCommandParser.kt, CommandRouter.kt, ActionValidator.kt; deterministic searches/URL encoding/HTTPS validation, preferred handlers with fallback and one-by-one previews. |
| 7 Screen gestures | accessibility/YesminAccessibilityService.kt, exact local gesture grammar, ViewModel countdown/one-shot dispatch, manifest config, Privacy disclosure and manual enable/disable. Model actions cannot invoke this path. |
| 8 Coding | Coding destination, coding/CodeBlocks.kt, chat system instruction, code copy/share, explanations/generation/debugging/formatting through Gemini. No executor. |
| 9 Settings/privacy | SettingsScreens.kt, data preferences, localized navigation, notes/reminders/calculator/history/shortcuts, retention default off, explicit preferences/clipboard/cloud consent, reset/stop/disable controls. |
| 10 Testing/build | 42 JUnit tests, pinned Gradle wrapper, GitHub Actions, build-linux and termux-cloud-build scripts, README/Firebase/Termux/test docs and build verification report. |

Feature completeness is bounded by platform setup: Gemini needs YOUR Firebase project, voice needs installed phone engines, gestures need explicit manual service enablement, reminders are inexact, and real-device acceptance testing remains mandatory. This is a usable first version, not a claim that every OEM or Google Play distribution requirement has already been certified.
