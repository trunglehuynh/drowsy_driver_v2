import SwiftUI

final class CameraCoordinator: ObservableObject {
    let permissionService: PermissionService
    let cameraService: CameraService
    let faceDetector: FaceDetectorService
    let drowsinessDetector: DrowsinessDetector
    let audioService: AudioAlertService
    let userInfoStore: UserInfoStore

    init(permissionService: PermissionService,
         cameraService: CameraService,
         faceDetector: FaceDetectorService,
         drowsinessDetector: DrowsinessDetector,
         audioService: AudioAlertService,
         userInfoStore: UserInfoStore) {
        self.permissionService = permissionService
        self.cameraService = cameraService
        self.faceDetector = faceDetector
        self.drowsinessDetector = drowsinessDetector
        self.audioService = audioService
        self.userInfoStore = userInfoStore
    }

    func start() -> some View {
        CameraView(viewModel: CameraViewModel(
            permissionService: self.permissionService,
            cameraService: cameraService,
            faceDetector: faceDetector,
            drowsinessDetector: drowsinessDetector,
            audioService: audioService,
            userInfoStore: userInfoStore
        ))
    }
}
