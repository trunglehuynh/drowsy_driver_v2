import SwiftUI
import AVFoundation

final class AppCoordinator: ObservableObject {
    private let permissionService = PermissionService()
    private let cameraService = CameraService()
    private let audioService = AudioAlertService()
    private let userInfoStore = UserInfoStore()
    private lazy var drowsinessDetector = DrowsinessDetector(userInfo: userInfoStore)
    private let purchases = PurchasesService()
    @Published private var route: Route = .permission

    private enum Route {
        case permission
        case disclaimer
        case paywall
        case camera
    }
    private let faceDetector: FaceDetectorService = MLKitFaceDetector.shared

    func start() -> some View {
        Group {
            switch initialRoute() {
            case .permission:
                PermissionView { [weak self] in self?.route = .disclaimer }
            case .disclaimer:
                DisclaimerView(onAgree: { [weak self] in
                    self?.userInfoStore.userAgreedDisclaimer()
                    self?.route = .camera
                })
            case .paywall:
                PaywallView(purchases: purchases) { [weak self] success in
                    self?.purchases.refresh()
                    self?.route = .camera
                }
            case .camera:
                CameraCoordinator(
                    permissionService: permissionService,
                    cameraService: cameraService,
                    faceDetector: faceDetector,
                    drowsinessDetector: drowsinessDetector,
                    audioService: audioService,
                    userInfoStore: userInfoStore
                ).start()
            }
        }
    }

    private func initialRoute() -> Route {
        let auth = AVCaptureDevice.authorizationStatus(for: .video)
        if auth != .authorized { return .permission }
        if !userInfoStore.isUserAgreedDisclaimer() { return .disclaimer }
        // Purchases/paywall parity (RevenueCat). Configure with API key from Info.plist if present.
        if let key = Bundle.main.object(forInfoDictionaryKey: "REVENUECAT_API_KEY") as? String, !key.isEmpty {
            purchases.configure(apiKey: key)
        }
        // Mirror Android gating by initial install version (> 17 requires paywall)
        if let bundleVersion = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String, let ver = Int(bundleVersion) {
            userInfoStore.mayUpdateInitialInstallVersion(currentVersion: ver)
        }
        #if canImport(RevenueCat)
        if userInfoStore.shouldShowPaywallForPreviousUsers() && purchases.status == .inactive {
            return .paywall
        }
        #endif
        return .camera
    }
}
