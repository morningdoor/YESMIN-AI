# Firebase AI Logic + App Check

## Production architecture

`GeminiRepository` calls `Firebase.ai(backend = GenerativeBackend.googleAI())` from the Firebase AI Logic Android SDK. It uses a system instruction, JSON MIME type and explicit JSON schema. The native router validates the parsed action list and obtains confirmation independently. A schema is NOT permission to execute. No Gemini secret API key is stored or logged by the app.

Official setup: https://firebase.google.com/docs/ai-logic/get-started
Official App Check guide: https://firebase.google.com/docs/ai-logic/app-check
Model list: https://firebase.google.com/docs/ai-logic/models

## Create your own project

1. Create/select a Firebase project in https://console.firebase.google.com/ . No Analytics SDK is required by this app.
2. Add Android app with package **com.yesmin.ai**. Download the app-specific `google-services.json`.
3. Put it in `app/google-services.json` (beside app/build.gradle.kts). Do not rename the package unless you update the entire project and register the new Firebase Android app.
4. In Firebase AI Logic, complete onboarding with the **Gemini Developer API** backend. Follow the current console prompts for enabling the required services; the console may provision the backend API key. Keep Gemini backend credentials out of the APK. Merely enabling a raw Gemini API somewhere else is not sufficient.
5. Review billing, region/model availability and quotas. Start with a supported text model and set project quotas/spend alerts appropriate for personal testing; alerts alone do not impose a hard spending cap.
6. Build and install the app; follow the debug or production App Check path below.
7. In YESMIN Privacy & Permissions, read and explicitly enable cloud AI consent. Review context/preference switches in Settings. In Gemini Configuration, connect, choose your supported model and tap Test.

The current model defaults are `gemini-3.8-flash` and the optional `gemini-3.5-flash-lite`, as listed by Firebase's supported-model documentation at preparation time. Use a supported replacement if the provider changes availability. Old Gemini 2.5 text models are restricted for some projects; do not assume a new Firebase project can use them. The app uses ordinary text generation and schema fields supported by the pinned client SDK; backend model access still needs a live test in your project.

## Private debug builds (personal testing only)

`app/src/debug/.../AppCheckInstaller.kt` installs DebugAppCheckProviderFactory. The debug artifact does NOT contain a hardcoded shared debug secret. On initialization/token use, the Firebase SDK prints its device debug token to local Android Logcat under `DebugAppCheckProvider`.

- Obtain the token only on your trusted device (Android Studio Logcat, or explicitly paired ADB as described in TERMUX_BUILD.md).
- Register it privately in Firebase console → App Check → your app → Manage debug tokens.
- Follow Firebase's current instructions to register the app/provider as needed, then enforce **Firebase AI Logic** App Check after testing.
- Keep enforcement ON during normal testing; authorize only your test token rather than broadly disabling protection.
- Never post the token in public screenshots/issues/logs. Revoke unused tokens.

Debug attestation is not suitable for distributed production builds. Someone with an authorized debug token may abuse it if it is leaked.

## Production release

`app/src/release/.../AppCheckInstaller.kt` uses PlayIntegrityAppCheckProviderFactory. The debug provider dependency is debugImplementation only.

1. Create a stable release signing key under your control. Store it privately, not in this ZIP/repository. Configure signing in your trusted build environment.
2. Register the release certificate SHA-256 in the Firebase Android app and set up the Play Integrity provider, including the requirements for your chosen distribution channel.
3. Use a production distribution/attestation setup supported by Firebase/Google Play. A random sideloaded unsigned or debug-signed release is NOT guaranteed to pass Play Integrity.
4. Verify App Check metrics, run real requests, and enable enforcement for Firebase AI Logic in the console. Missing backend enforcement cannot be detected reliably by this app; the UI does not claim to prove it.
5. Configure quota/rate-limit controls, spend monitoring and any additional auth/backend restrictions your threat model needs. App Check alone is not user authentication or an unlimited-abuse guarantee.
6. Complete privacy disclosures and Google Play accessibility/API policy review before distribution. Do not market this general assistant as a disability tool.

## Configuration management

The app can select/save a model ID, test, disconnect locally, reset Gemini switches and display the configured project ID. It cannot rewrite its compiled Firebase project resources securely from a random runtime API-key entry. To replace/delete the Firebase project config, replace/remove `app/google-services.json` (and update/remove the CI secret), then rebuild/reinstall. Local disconnect cancels active jobs and gates all new AI calls; it does not revoke the backend project or delete remote records.

Firebase's client configuration includes a Firebase API key, which identifies the project and is not the secret Gemini backend key. It is normally embedded in Android apps; do not mistake hiding it for backend security. This project keeps the downloaded file out of source control for clean configuration separation. Use API restrictions, App Check enforcement, quotas and the documented Firebase architecture.

## Error privacy

Application code does not log prompts, raw model responses, API keys or preferences. UI errors use bounded generic descriptions and exception class names. The Firebase debug SDK's local debug-token log is necessary for private development registration; treat that token as sensitive. No crash/analytics-upload SDK is included. Share only redacted troubleshooting details.

No Firebase credentials were supplied during project creation, so an end-to-end live Gemini/App Check test cannot truthfully be claimed. Use the in-app connection test after completing the steps above.
