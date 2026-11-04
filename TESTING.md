# Verification and acceptance tests

## Automated checks

```
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

The repository contains 42 pure JVM tests covering strict action validation, unsupported/unsafe schemes, credentials/local URLs/custom ports, too many actions, injected shell/gesture actions, strict JSON fields, multilingual command forms, browser/YouTube multi-step collapse, case-preserving URLs, exact gesture grammar, arithmetic and code-block extraction.

See BUILD_VERIFICATION.md for results actually obtained in this delivery. Unit tests are not real-device tests. The Android lint HTML and unit-test HTML reports are uploaded by CI. The release build compiles the production App Check source set but is unsigned unless you configure signing.

## Manual device acceptance matrix — perform before trusting actions

| Area | Steps / expected result |
|---|---|
| First start without Firebase | Home opens, navigation/local tools work; Gemini test reports missing config, does not crash. |
| Cloud consent off | Natural chat/test blocked with clear explanation; deterministic local commands can still present previews. |
| Firebase configured | Register your debug token, enable enforcement, consent, test; verify a real response. Try an invalid token/project/model to verify errors. |
| Conversation | Ask in English, Hindi, Bangla and Hinglish; select language; verify replies/context, cancel an active request, clear mid-request and ensure it cannot repopulate old history. |
| Preferences | Save a preference, leave share switch off and inspect request construction; only enable when approved. Forget preference. |
| Retention | Defaults off. Kill/relaunch app: unsaved chat/history absent, explicit notes/preferences retained. Opt in, chat, restart, clear and verify deletion. |
| Voice denial | Reject microphone; text input remains usable and there is no recording. |
| Voice lifecycle | Tap mic, observe visible state and Android indicator, stop, navigate away/background the app; mic must stop. No continuous/wake-word capture. |
| Speech service missing | Disable installed recognition service; app shows useful error. |
| TTS | Install/select language and voices; preview/rate/stop; no guaranteed female metadata. Verify off switch blocks speech. |
| App launcher | Search text/voice, tap a real app, mark favourite, verify recent ordering; disabled/missing apps explain failure. Refresh after installation/removal. |
| Intent commands | `Yesmin, Chrome kholo`, `Claude app open karo`, `Settings mein jao` (natural Gemini route), `Calculator open karo`; no unrequested setting changes. |
| Website | `Open example.com/AbCd in Chrome`; inspect case-preserving URL preview. No browser -> clear error. Preferred Chrome unavailable -> compatible handler/browser fallback. |
| YouTube | `YouTube par relaxing music search karo`; installed app opens encoded results. With app absent, browser fallback. User chooses playback; no autoplay claim. |
| Multi-step | `Chrome kholo aur Google par weather search karo` collapses to one search. For a Gemini multi-action list confirm one action, return and confirm next; cancel stops remaining steps. |
| Reject automation | Model `SHELL`, `TAP`, `SEND_SMS`, file deletion, `javascript:` and >3 actions rejected. Quoted historical text must not authorize an action. Review every preview. |
| Accessibility disabled | `swipe up` explains permission; service cannot be enabled silently. Android and app consent switches both required. |
| Accessibility disclosure | Read disclosure, manually enable OS service. It is explicitly not declared an accessibility disability tool and does not request screen content. |
| One gesture | Put an innocuous app behind YESMIN; request `swipe up`, confirm, cancel countdown once, then retry and allow one gesture. Verify one dispatch only. Test invalid/offscreen tap coords and disabled service. Never test on authentication/payment/deletion controls. |
| Coding | Ask for HTML/Python/Kotlin, verify fenced code rendering, copy/share only on button press. Ask to format/debug code. No execution; open your chosen coding app via launcher. |
| Clipboard | Open Notes & Tools, press Paste clipboard. Nothing sends automatically; no background clipboard read. |
| Calculator | `(12+3)*4/2` -> 30; malformed/divide-by-zero rejected. |
| Notes | Save, restart, copy/share explicitly; delete confirmation. |
| Reminder | Enable notifications, schedule a few minutes ahead, permit delay; test cancel and reboot rescheduling of future entries. Force-stop/Doze can prevent/delay notifications. |
| Appearance | Dark/light/system modes, font scaling, rotation, keyboard, small screen; navigate core labels in hi/bn/en. Some secondary technical copy remains English. |
| Reset | Confirm reset: notes/history/preferences/shortcuts/favourites gone, reminders cancelled, voice/request stopped, accessibility service disabled. Compiled Firebase config remains until rebuild. |

Real-device UI/engine/OEM behavior, Firebase quotas and attestation are not proven by an APK compilation. Record your device model, Android version, speech/TTS engine, build/signing variant and redacted result when reporting issues. Never include API/debug tokens, private prompts or screenshots of authentication screens.
