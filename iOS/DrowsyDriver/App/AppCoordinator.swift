import SwiftUI
import AVFoundation

final class AppCoordinator: ObservableObject {
    private let permissionService = PermissionService()
    private let cameraService = CameraService()
    private let audioService = AudioAlertService()
    private let userInfoStore = UserInfoStore()
    private lazy var drowsinessDetector = DrowsinessDetector(userInfo: userInfoStore)
    @Published private var route: Route = .permission

    private enum Route {
        case permission
        case disclaimer
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
        return .camera
    }
}
