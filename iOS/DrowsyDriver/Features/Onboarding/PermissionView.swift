import SwiftUI
import AVFoundation
import UIKit

struct PermissionView: View {
    var onGranted: () -> Void
    @State private var status = AVCaptureDevice.authorizationStatus(for: .video)

    var body: some View {
        VStack(spacing: 16) {
            Text("Camera Permission Required").font(.title2).bold()
            Text("We use the front camera to detect drowsiness on-device. No images are stored or sent.")
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            Button(action: request) {
                Text(status == .denied ? "Open Settings" : "Allow Camera")
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Color.accentColor)
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
            }
        }
        .padding()
    }

    private func request() {
        switch status {
        case .authorized:
            onGranted()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                DispatchQueue.main.async {
                    self.status = AVCaptureDevice.authorizationStatus(for: .video)
                    if granted { onGranted() }
                }
            }
        default:
            if let url = URL(string: UIApplication.openSettingsURLString) {
                UIApplication.shared.open(url)
            }
        }
    }
}

