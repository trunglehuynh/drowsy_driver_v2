import Foundation

struct DetectedFaceInfo {
    let hasFace: Bool
    let leftEyeOpenProbability: Float?
    let rightEyeOpenProbability: Float?
    let boundingBox: CGRect?
    let imageSize: CGSize?
}

final class DrowsinessDetector: ObservableObject {
    // Parity with Android UserInfo defaults
    private(set) var sensitiveThresholdPercent: Int
    private(set) var durationThresholdMs: Int64
    private(set) var alertOnEmptyFace: Bool

    // Empty-face alert gaps (non-PiP and PiP)
    private let emptyFaceAlertGap: Int64 = 5000
    private let emptyFaceAlertGapInPip: Int64 = 10000
    private var isInPictureInPictureMode: Bool = false

    // State
    private var lastTimeEyesClosed: Int64?
    private var lastTimeDetectFace: Int64?

    init(userInfo: UserInfoStore) {
        self.sensitiveThresholdPercent = userInfo.getSensitiveThreshold()
        self.durationThresholdMs = userInfo.getDurationThreshold()
        self.alertOnEmptyFace = userInfo.getAlertEmptyFace()
    }

    func setSensitiveThreshold(_ value: Int) { sensitiveThresholdPercent = value }
    func setDurationThreshold(_ ms: Int64) { durationThresholdMs = ms }
    func setAlertOnEmptyFace(_ enabled: Bool) { alertOnEmptyFace = enabled }
    func setPiPMode(_ enabled: Bool) { isInPictureInPictureMode = enabled }

    func evaluate(_ info: DetectedFaceInfo) -> (shouldAlert: Bool, isDrowsy: Bool, showEmptyFaceAlert: Bool) {
        let now = Int64(Date().timeIntervalSince1970 * 1000)

        // No face detected
        if !info.hasFace {
            // Keep lastTimeEyesClosed unchanged to tolerate missed frames
            if !alertOnEmptyFace { return (false, false, false) }

            let gap = isInPictureInPictureMode ? emptyFaceAlertGapInPip : emptyFaceAlertGap
            if let last = lastTimeDetectFace, (now - last) > gap {
                return (true, false, true)
            } else {
                return (false, false, false)
            }
        }

        lastTimeDetectFace = now

        // Eye closed check mirrors Android logic
        let threshold = Float(sensitiveThresholdPercent) / 100.0
        let isClosed: Bool = {
            guard let l = info.leftEyeOpenProbability, let r = info.rightEyeOpenProbability else { return false }
            return (l <= threshold) && (r <= threshold)
        }()

        if !isClosed {
            lastTimeEyesClosed = nil
            return (false, false, false)
        }

        if lastTimeEyesClosed == nil { lastTimeEyesClosed = now; return (false, true, false) }
        let elapsed = now - (lastTimeEyesClosed ?? now)
        let shouldAlert = elapsed >= durationThresholdMs
        return (shouldAlert, true, false)
    }
}

