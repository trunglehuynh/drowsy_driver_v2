import SwiftUI
import AVFoundation

struct CameraView: View {
    @StateObject var viewModel: CameraViewModel
    @State private var showSettings = false

    var body: some View {
        ZStack {
            CameraPreviewLayer(cameraService: viewModel.cameraService)
                .ignoresSafeArea()
            if let box = viewModel.faceBoundingBox {
                FaceBoxOverlay(box: box, isMirrored: viewModel.cameraService.isMirrored)
            }
            if viewModel.showDrowsyOverlay {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 64))
                    .foregroundColor(.yellow)
                    .padding()
                    .background(.black.opacity(0.4))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            if viewModel.showEmptyFaceAlert {
                Text("No face detected")
                    .padding(8)
                    .background(.black.opacity(0.5))
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .padding(.top, 60)
                    .frame(maxHeight: .infinity, alignment: .top)
            }
            VStack {
                HStack {
                    Spacer()
                    Button {
                        showSettings = true
                    } label: {
                        Image(systemName: "gearshape")
                            .padding(10)
                            .background(.black.opacity(0.4))
                            .clipShape(Circle())
                            .foregroundColor(.white)
                    }
                    Button {
                        viewModel.toggleScreenLight()
                    } label: {
                        Image(systemName: viewModel.isScreenLightOn ? "sun.max.fill" : "sun.max")
                            .padding(10)
                            .background(.black.opacity(0.4))
                            .clipShape(Circle())
                            .foregroundColor(.white)
                    }
                }
                Spacer()
            }
            if viewModel.isScreenLightOn {
                Color.white.opacity(0.7).ignoresSafeArea()
            }
        }
        .onAppear { viewModel.onAppear() }
        .onDisappear { viewModel.onDisappear() }
        .sheet(isPresented: $showSettings) {
            SettingsView(viewModel: SettingsViewModel(userInfo: viewModel.userInfoStore, audio: viewModel.audioService))
        }
    }
}

private struct CameraPreviewLayer: UIViewRepresentable {
    let cameraService: CameraService
    func makeUIView(context: Context) -> PreviewView { PreviewView(cameraService: cameraService) }
    func updateUIView(_ uiView: PreviewView, context: Context) {}
}

private final class PreviewView: UIView {
    private let previewLayer: AVCaptureVideoPreviewLayer
    init(cameraService: CameraService) {
        self.previewLayer = cameraService.makePreviewLayer()
        super.init(frame: .zero)
        layer.addSublayer(previewLayer)
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }
    override func layoutSubviews() {
        super.layoutSubviews()
        previewLayer.frame = bounds
    }
}

private struct FaceBoxOverlay: View {
    let box: CGRect
    let isMirrored: Bool
    var body: some View {
        GeometryReader { geo in
            var x = box.origin.x
            if isMirrored { x = 1.0 - box.origin.x - box.size.width }
            let rect = CGRect(
                x: x * geo.size.width,
                y: box.origin.y * geo.size.height,
                width: box.size.width * geo.size.width,
                height: box.size.height * geo.size.height
            )
            Path { path in
                path.addRect(rect)
            }
            .stroke(.green, lineWidth: 4)
        }
    }
}
