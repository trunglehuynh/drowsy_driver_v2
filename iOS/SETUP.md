# iOS Setup (DrowsyDriver)

- Requirements: Xcode 15+, iOS 16+ target, CocoaPods installed.
- Dependencies: Google ML Kit Face Detection.

Steps
- Create a new Xcode iOS App project named "DrowsyDriver" pointing to `iOS/DrowsyDriver`.
- Add the Swift files under `App/`, `Coordinators/`, `Domain/`, `Features/`, `Services/`, `Storage/` to the app target.
- Add Info.plist key: `NSCameraUsageDescription = Camera is used to detect drowsiness while driving.`
- Install ML Kit:
  - `cd iOS/DrowsyDriver && pod install`
  - Open the generated `.xcworkspace`.
- Sounds: Copy alert audio (e.g., `short_alert.wav`) from Android assets to the app bundle (Resources). Keep filenames identical.
- Capabilities: Background modes not required; keep screen awake if desired via app logic.
- Build & Run on a real device for camera access.

Parity Notes
- Thresholds: sensitive=90, duration=500ms, empty-face alert off by default.
- Frame rate: process ~10 FPS (throttled).
- Face filtering: consider largest face with min bounding box ~100 px (apply in ML Kit results).

