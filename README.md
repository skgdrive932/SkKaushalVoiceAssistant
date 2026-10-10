# SK Kaushal Voice Assistant

Android voice-assistant starter project using Kotlin, Android SpeechRecognizer, Android Text-to-Speech, Gemini API, and GitHub Actions APK builds.

## Features
- Hindi/English speech input (recognition availability and language support depend on the device's speech service).
- Spoken answers through Android Text-to-Speech.
- Gemini-powered answers when a key is supplied.
- Local intents for Android Settings, Wi-Fi settings, Bluetooth settings, Camera, greeting, and current time.
- “Hey SK” phrase detection while the app is open.
- GitHub Actions builds a debug APK and uploads it as an artifact.

## Important limitations
- This is a working starter implementation, not a fully tested commercial assistant.
- “Hey SK” uses Android's speech-recognition service and only attempts wake phrase listening while the activity is open. It is **not** a reliable always-on, locked-screen/background hotword engine. Android restricts background microphone services, and true low-power hotword detection usually requires a dedicated on-device keyword-spotting engine and careful device testing.
- Speech recognition availability, Hindi recognition, TTS voice quality, and app launching vary by phone/OS.
- Phone actions are intentionally limited to safe Android intents. The app does not silently send messages, place calls, toggle restricted settings, or control other apps without the platform's permissions/user interaction.
- Gemini is a cloud service; requests require internet and may incur usage costs.

## Gemini API key
1. Create/manage a key in Google AI Studio.
2. Run the app and enter the key in the password field.
3. The key is held only in the current app session; it is not written to disk by this sample.

**Security:** A key embedded in a mobile app can be extracted. For production, route Gemini requests through a backend you control rather than shipping a long-lived secret in the APK. Never commit a real key to GitHub.

## Build APK with GitHub Actions
1. Create a GitHub repository named `SkKaushalVoiceAssistant`.
2. Upload/push all project files to the repository root (the `.github/workflows/android-build.yml` file must remain in that path).
3. Open **Actions** → **Android APK Build** → **Run workflow**, or push to `main`/`master`.
4. Open the completed workflow run → **Artifacts** → download `SK-Kaushal-VoiceAssistant-debug-apk`.
5. Extract the downloaded artifact ZIP; it contains `app-debug.apk`.

The workflow uses JDK 17 and Gradle 8.9. You do not need to commit `local.properties` or a Gemini key.

## Build locally
Install Android Studio with Android SDK 35 and JDK 17, open the project, then sync and run the `app` configuration. The GitHub Actions workflow can build without a committed Gradle wrapper.


## Fixed assistant identity
The app's assistant name is hard-coded as **Sk Kaushal Voice Assistant** in the UI and Gemini system instruction. There is no in-app setting to rename it. As with any Android app, someone who modifies the source code and rebuilds the APK could change the name; no app can prevent its own developer/source from being edited.
