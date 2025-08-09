import Combine
import CoreGraphics
import Foundation
import SwiftUI

final class CameraViewModel: NSObject, ObservableObject {
    // Outputs
    @Published var showDrowsyOverlay: Bool = false
    @Published var showEmptyFaceAlert: Bool = false
    @Published var faceBoundingBox: CGRect? = nil
    @Published var isScreenLightOn: Bool = false

    private let permissionService: PermissionService
    let cameraService: CameraService
    private let faceDetector: FaceDetectorService
    private let drowsinessDetector: DrowsinessDetector
    let audioService: AudioAlertService
    let userInfoStore: UserInfoStore

    // Frame rate limiting (match Android ~10 FPS)
    private let gapBetweenFramesMs: Int64 = 100
    private var lastFrameTimeMs: Int64 = 0

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

    func onAppear() {
        permissionService.requestCamera { [weak self] granted in
            guard let self else { return }
            if granted {
                self.cameraService.delegate = self
                self.cameraService.start()
            }
        }
    }

    func onDisappear() {
        cameraService.stop()
    }

    func toggleScreenLight() { isScreenLightOn.toggle() }
}

extension CameraViewModel: CameraServiceDelegate {
    func cameraService(_ service: CameraService, didOutput sampleBuffer: CMSampleBuffer, orientation: CGImagePropertyOrientation) {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        if now - lastFrameTimeMs < gapBetweenFramesMs { return }
        lastFrameTimeMs = now
        faceDetector.detect(sampleBuffer: sampleBuffer, orientation: orientation) { [weak self] info in
            guard let self else { return }
            let result = self.drowsinessDetector.evaluate(info)
            DispatchQueue.main.async {
                self.showDrowsyOverlay = result.isDrowsy
                self.showEmptyFaceAlert = result.showEmptyFaceAlert
                if result.shouldAlert { self.audioService.alert() }
                self.faceBoundingBox = info.boundingBox
            }
        }
    }
}
