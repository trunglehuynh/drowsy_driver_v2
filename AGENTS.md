# Repository Guidelines

## Project Structure & Module Organization
- Android app lives in `Android/app`.
- Source code: `Android/app/src/main/java/com/galaxylab/drowsydriver` with feature packages: `UI/`, `Notification/`, `AI/`, `Utility/`.
- Resources: `Android/app/src/main/res` (layouts, drawables, menus, values). Raw sounds in `res/raw/`.
- Tests: unit in `Android/app/src/test`, instrumentation in `Android/app/src/androidTest`.
- iOS folder exists but is currently empty.

## Build, Test, and Development Commands
- Build debug APK: `cd Android && ./gradlew assembleDebug`.
- Install on device/emulator: `./gradlew installDebug` (device connected).
- Run unit tests: `./gradlew testDebugUnitTest`.
- Run UI/instrumented tests: `./gradlew connectedDebugAndroidTest` (emulator running).
- Clean project: `./gradlew clean`.
- Open with Android Studio for IDE-driven run/debug and logcat.

## Coding Style & Naming Conventions
- Language: Kotlin (AndroidX), 4-space indent, no tabs.
- Classes/objects: `PascalCase`; methods/fields: `camelCase`; constants: `UPPER_SNAKE_CASE`.
- Packages: lowercase (e.g., `com.galaxylab.drowsydriver`).
- Resources: `snake_case` (e.g., `activity_main.xml`, `ic_alarm.png`).
- Use ViewBinding (enabled) instead of `findViewById`; prefer Kotlin coroutines; DI via Koin.

## Testing Guidelines
- Frameworks: JUnit4 (`src/test`) and AndroidX Test/Espresso (`src/androidTest`).
- Name tests after subject and behavior (e.g., `FaceDetectorTest`, `CameraFragmentTest`).
- Aim coverage on core modules: `AI/FaceDetector`, `UI/CameraFragment`, `Utility/*`.
- Keep tests deterministic; avoid real network or camera—use fakes/mocks.

## Commit & Pull Request Guidelines
- Commits: concise imperative subject (e.g., `fix: prevent crash in CameraFragment`).
- Reference issues in body when applicable; group related changes per commit.
- PRs: include summary, screenshots for UI, steps to test, and linked issues.
- Ensure build passes locally with `./gradlew assembleDebug` and tests are green.

## Security & Configuration Tips
- Do not commit secrets. Keep signing keys and service JSON private; `google-services.json` must be valid for Firebase features.
- Local SDK path lives in `Android/local.properties`. Signing uses a local keystore; update `signingConfigs` paths only locally.

