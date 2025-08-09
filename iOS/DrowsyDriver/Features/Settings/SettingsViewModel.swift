import Foundation

final class SettingsViewModel: ObservableObject {
    @Published var sensitivity: Int
    @Published var durationMs: Int
    @Published var alertOnEmptyFace: Bool
    @Published var selectedSound: String
    let availableSounds: [String] = [
        "short_alert.wav",
        "red_alert.wav",
        "notification_up.wav",
        "cartoon_alert.mp3",
        "pup_alert.mp3",
        "sweet_alert.wav",
        "men_alert.wav"
    ]

    private let userInfo: UserInfoStore
    private let audio: AudioAlertService

    init(userInfo: UserInfoStore, audio: AudioAlertService) {
        self.userInfo = userInfo
        self.audio = audio
        self.sensitivity = userInfo.getSensitiveThreshold()
        self.durationMs = Int(userInfo.getDurationThreshold())
        self.alertOnEmptyFace = userInfo.getAlertEmptyFace()
        self.selectedSound = audio.lastSoundName()
    }

    func save() {
        userInfo.updateSensitiveThreshold(sensitivity)
        userInfo.updateDurationThreshold(Int64(durationMs))
        userInfo.updateAlertEmptyFace(alertOnEmptyFace)
    }

    func pickSound(_ name: String) {
        selectedSound = name
        audio.setAlarmSound(name)
    }
}

