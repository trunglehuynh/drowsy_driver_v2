import CoreGraphics
import CoreImage
import Foundation
import UIKit
import AVFoundation
#if canImport(MLKitVision)
import MLKitVision
#endif
#if canImport(MLKitFaceDetection)
import MLKitFaceDetection
#endif

protocol FaceDetectorService {
    func detect(sampleBuffer: CMSampleBuffer, orientation: CGImagePropertyOrientation, completion: @escaping (DetectedFaceInfo) -> Void)
}

// Placeholder ML Kit-backed detector. Wire to ML Kit Face Detection via CocoaPods.
final class MLKitFaceDetector: FaceDetectorService {
    static let shared = MLKitFaceDetector()
    private init() {}

    func detect(sampleBuffer: CMSampleBuffer, orientation: CGImagePropertyOrientation, completion: @escaping (DetectedFaceInfo) -> Void) {
#if canImport(MLKitVision) && canImport(MLKitFaceDetection)
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else {
            completion(DetectedFaceInfo(hasFace: false, leftEyeOpenProbability: nil, rightEyeOpenProbability: nil, boundingBox: nil, imageSize: nil))
            return
        }
        let width = CGFloat(CVPixelBufferGetWidth(pixelBuffer))
        let height = CGFloat(CVPixelBufferGetHeight(pixelBuffer))

        let options = FaceDetectorOptions()
        options.performanceMode = .fast
        options.landmarkMode = .none
        options.contourMode = .none
        options.classificationMode = .all
        options.minFaceSize = 0.5
        options.isTrackingEnabled = true

        let detector = FaceDetector.faceDetector(options: options)
        let image = VisionImage(buffer: sampleBuffer)
        image.orientation = uiImageOrientation(from: orientation)

        detector.process(image) { faces, error in
            guard error == nil, let faces = faces, !faces.isEmpty else {
                completion(DetectedFaceInfo(hasFace: false, leftEyeOpenProbability: nil, rightEyeOpenProbability: nil, boundingBox: nil, imageSize: CGSize(width: width, height: height)))
                return
            }
            // Choose largest face with min 100px size (Android parity)
            let filtered = faces.filter { $0.frame.width >= 100 && $0.frame.height >= 100 }
            let face = (filtered.max { min($0.frame.width, $0.frame.height) < min($1.frame.width, $1.frame.height) }) ?? faces[0]
            let l = face.leftEyeOpenProbability?.floatValue
            let r = face.rightEyeOpenProbability?.floatValue
            // Normalize bounding box [0,1] for overlay; Vision frame is in image coords
            let rect = face.frame
            let norm = CGRect(x: rect.origin.x / width,
                              y: rect.origin.y / height,
                              width: rect.size.width / width,
                              height: rect.size.height / height)
            completion(DetectedFaceInfo(hasFace: true, leftEyeOpenProbability: l, rightEyeOpenProbability: r, boundingBox: norm, imageSize: CGSize(width: width, height: height)))
        }
#else
        completion(DetectedFaceInfo(hasFace: false, leftEyeOpenProbability: nil, rightEyeOpenProbability: nil, boundingBox: nil, imageSize: nil))
#endif
    }
}


private func uiImageOrientation(from cgOrientation: CGImagePropertyOrientation) -> UIImage.Orientation {
    switch cgOrientation {
    case .up: return .up
    case .upMirrored: return .upMirrored
    case .down: return .down
    case .downMirrored: return .downMirrored
    case .left: return .left
    case .leftMirrored: return .leftMirrored
    case .right: return .right
    case .rightMirrored: return .rightMirrored
    @unknown default: return .right
    }
}
