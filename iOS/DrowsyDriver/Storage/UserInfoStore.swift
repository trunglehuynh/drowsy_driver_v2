import Foundation

final class UserInfoStore {
    private let defaults = UserDefaults.standard

    // Keys mirror Android names where applicable
    private let spSensitive = "SP_SENSITIVE_THRESHOLD_ALERT"
    private let spDuration = "SP_DURATION_THRESHOLD_ALERT"
    private let spEmptyFace = "ALERT_EMPTY_FACE"
    private let agreedDisclaimer = "AGREED_DISCLAIMER"

    // Android defaults
    private let defaultSensitive = 90
    private let minSensitive = 10
    private let maxSensitive = 95
    private let defaultDuration: Int64 = 500
    private let minDuration: Int64 = 100
    private let maxDuration: Int64 = 5000
    private let defaultEmptyFace = false
    private let defaultAgreed = false

    func getSensitiveThreshold() -> Int {
        let value = defaults.integer(forKey: spSensitive)
        return value == 0 ? defaultSensitive : value
    }

    func updateSensitiveThreshold(_ value: Int) {
        let clamped = max(minSensitive, min(maxSensitive, value))
        defaults.set(clamped, forKey: spSensitive)
    }

    func getDurationThreshold() -> Int64 {
        let value = defaults.object(forKey: spDuration) as? Int64
        return value ?? defaultDuration
    }

    func updateDurationThreshold(_ value: Int64) {
        let clamped = max(minDuration, min(maxDuration, value))
        defaults.set(clamped, forKey: spDuration)
    }

    func getAlertEmptyFace() -> Bool {
        if defaults.object(forKey: spEmptyFace) == nil { return defaultEmptyFace }
        return defaults.bool(forKey: spEmptyFace)
    }

    func updateAlertEmptyFace(_ value: Bool) { defaults.set(value, forKey: spEmptyFace) }

    // Disclaimer
    func isUserAgreedDisclaimer() -> Bool {
        if defaults.object(forKey: agreedDisclaimer) == nil { return defaultAgreed }
        return defaults.bool(forKey: agreedDisclaimer)
    }
    func userAgreedDisclaimer() { defaults.set(true, forKey: agreedDisclaimer) }
}
