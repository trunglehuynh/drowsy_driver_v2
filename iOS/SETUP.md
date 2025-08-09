# iOS Setup (DrowsyDriver)

- Requirements: Xcode 15+, iOS 16+ target, CocoaPods installed.
- Dependencies: Google ML Kit Face Detection.

Steps
- Generate Xcode project via XcodeGen:
  - Install: `brew install xcodegen`
  - `cd iOS/DrowsyDriver && xcodegen generate`
  - This creates `DrowsyDriver.xcodeproj` using `project.yml` and `Info.plist`.
- Install ML Kit and RevenueCat (CocoaPods):
  - `pod install`
  - Open the generated `DrowsyDriver.xcworkspace` in Xcode.
- Sounds: Copy alert audio (e.g., `short_alert.wav`) from Android assets to the app bundle (Resources). Keep filenames identical.
- App Icon: Drop iPhone/iPad icons into `Resources/Assets.xcassets/AppIcon.appiconset` (a template was added). Xcode will guide missing sizes.
- Capabilities: Background modes not required; keep screen awake if desired via app logic.
- Build & Run on a real device for camera access.

Parity Notes
- Thresholds: sensitive=90, duration=500ms, empty-face alert off by default.
- Frame rate: process ~10 FPS (throttled).
- Face filtering: consider largest face with min bounding box ~100 px (apply in ML Kit results).
