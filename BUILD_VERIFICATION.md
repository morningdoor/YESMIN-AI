# Actual build verification — YESMIN AI v1.0.0

## Environment and successful gates

Host: Linux x86-64; full OpenJDK 17; Gradle 8.11.1; Android Gradle Plugin 8.9.2; official Android SDK platform 35/build-tools 35.0.0. The Firebase client config was intentionally absent: no project credentials were supplied. Dependencies were actually resolved from their repositories.

| Check | Actual result |
|---|---|
| Final debug Kotlin/Java compilation and APK assembly | PASS — BUILD SUCCESSFUL |
| Pure JVM unit tests | PASS — 42 tests, 0 failures, 0 errors, 0 skipped |
| Release variant and production App Check source set | PASS — assembleRelease; unsigned until you configure signing |
| Android debug lint | PASS — zero Error/Fatal issues; 15 warning/information issues remain |
| APK signature | PASS — apksigner verify, Android debug certificate, v2 signature |
| APK identity | com.yesmin.ai; version 1.0.0 (1) |
| SDK levels from actual APK | minimum 26; target 35; compile 35 |
| Both Bash build scripts | bash -n syntax checks passed |
| Native Termux host assumption | Standard SDK AAPT2 identified as x86-64 glibc; not misrepresented as native ARM Termux |

Unit-test suites:

- com.yesmin.ai.ActionValidatorTest: 16 passed
- com.yesmin.ai.CalculatorTest: 8 passed
- com.yesmin.ai.LocalCommandParserTest: 18 passed

Lint issue counts (these are disclosed rather than hidden behind a baseline):

- AppBundleLocaleChanges: 1
- ComposableNaming: 1
- DataExtractionRules: 1
- GradleDependency: 6
- UnusedAttribute: 1
- UseKtx: 5

A small low-memory host required splitting final debug, release and lint verification into separate Gradle invocations and constraining the JVM. All three final invocations succeeded. Earlier syntax/build-environment errors were corrected. Normal CI has more memory; the workflow also runs these gates. Two Kotlin auto-mirrored-icon deprecation warnings and any native-symbol strip notices are not compile failures; native libraries are packaged unchanged.

Final delivered debug APK SHA-256:

```
d29de1021fba574d2ab980b561a87da48e63bfd8702d4d89d79298912de6b282
```

The separately supplied `YESMIN-AI-local-debug.apk` is an actual signed APK built from this final source snapshot, but has NO Firebase project configuration. It supports local-tool/UI testing, not a preconfigured live AI account. To enable cloud AI, add your own Firebase config, configure App Check and rebuild. Never ship this debug key/provider as a production security setup.

## Exact local verification commands

The final host used full JDK 17 and these Gradle tasks in separate processes:

```
:app:testDebugUnitTest :app:assembleDebug
:app:assembleRelease
:app:lintDebug
```

For the constrained verification host only, JVM options were `-Xmx768m -XX:MaxMetaspaceSize=384m -XX:ReservedCodeCacheSize=128m -XX:ActiveProcessorCount=2 -XX:+UseSerialGC` with one worker. The source defaults are suitable for the larger CI runner. Gradle stdout logs and actual JUnit/lint XML are in `verification/`. APK signature verification output is included there as well.

## NOT claimed as tested

- No real Android phone/emulator UI session was available. Microphone, speech/TTS engines, intent resolution on specific OEMs, accessibility gestures, reminders across reboot/Doze, and layout/permission behavior require the manual device matrix in TESTING.md.
- No Firebase credentials were supplied: live Gemini responses, project quota/billing and real App Check enforcement/attestation have NOT been tested. The SDK integration and both source variants compile; that is not an end-to-end connection claim.
- No GitHub account was used to publish/execute your remote workflow. The included workflow encodes the same Linux build/test path and the Termux scripts passed syntax checks; remote Actions/account/permissions/quota must be verified by you.
- ARM/Bionic Termux native SDK compilation has NOT been performed or promised. The documented supported alternative is phone-managed GitHub Actions compilation.
- No Google Play approval, production signing/attestation or complete secondary-copy localization is claimed.
