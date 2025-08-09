import AVFoundation
import Foundation

final class AudioAlertService {
    private var player: AVAudioPlayer?
    private let defaults = UserDefaults.standard
    private let lastSoundKey = "LAST_SOUND_URL"
    private let defaultSound = "short_alert.wav"

    init() {
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: [.duckOthers])
        try? AVAudioSession.sharedInstance().setActive(true, options: [])
        setAlarmSound(lastSoundName())
    }

    func lastSoundName() -> String { defaults.string(forKey: lastSoundKey) ?? defaultSound }

    func setAlarmSound(_ name: String) {
        do {
            if name.hasPrefix("http://") || name.hasPrefix("https://") || name.hasPrefix("file://") {
                let url = URL(string: name)!
                player = try AVAudioPlayer(contentsOf: url)
            } else if let url = Bundle.main.url(forResource: (name as NSString).deletingPathExtension,
                                               withExtension: (name as NSString).pathExtension) {
                player = try AVAudioPlayer(contentsOf: url)
            } else if let url = Bundle.main.url(forResource: (defaultSound as NSString).deletingPathExtension,
                                               withExtension: (defaultSound as NSString).pathExtension) {
                player = try AVAudioPlayer(contentsOf: url)
            }
            player?.prepareToPlay()
            defaults.setValue(name, forKey: lastSoundKey)
        } catch {
            // Fallback to default sound if any error
            if let url = Bundle.main.url(forResource: (defaultSound as NSString).deletingPathExtension,
                                         withExtension: (defaultSound as NSString).pathExtension) {
                player = try? AVAudioPlayer(contentsOf: url)
                player?.prepareToPlay()
            }
        }
    }

    func alert() {
        guard let player = player else { return }
        if player.isPlaying { return }
        player.numberOfLoops = 0
        player.play()
    }
}

