import SwiftUI
import UniformTypeIdentifiers
import UIKit

struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject var viewModel: SettingsViewModel

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Sensitivity (%)")) {
                    HStack {
                        Slider(value: Binding(get: { Double(viewModel.sensitivity) }, set: { viewModel.sensitivity = Int($0) }), in: 10...95, step: 1)
                        Text("\(viewModel.sensitivity)")
                            .frame(width: 40, alignment: .trailing)
                    }
                }
                Section(header: Text("Duration (ms)")) {
                    HStack {
                        Slider(value: Binding(get: { Double(viewModel.durationMs) }, set: { viewModel.durationMs = Int($0) }), in: 100...5000, step: 50)
                        Text("\(viewModel.durationMs)")
                            .frame(width: 60, alignment: .trailing)
                    }
                }
                Section {
                    Toggle("Alert when no face detected", isOn: $viewModel.alertOnEmptyFace)
                }
                Section(header: Text("Alert Sound")) {
                    ForEach(viewModel.availableSounds, id: \.self) { sound in
                        HStack {
                            Text(sound)
                            Spacer()
                            if sound == viewModel.selectedSound {
                                Image(systemName: "checkmark")
                            }
                        }
                        .contentShape(Rectangle())
                        .onTapGesture { viewModel.pickSound(sound) }
                    }
                    Button {
                        importSound()
                    } label: {
                        Label("Import from Files…", systemImage: "tray.and.arrow.down")
                    }
                }
            }
            .navigationTitle("Settings")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { viewModel.save(); dismiss() }
                }
            }
        }
    }
    
    private func importSound() {
        DocumentPicker.pickAudio { url in
            guard let url = url else { return }
            viewModel.pickSound(url.absoluteString)
        }
    }
}

 

enum DocumentPicker {
    static func pickAudio(_ completion: @escaping (URL?) -> Void) {
        let controller = UIDocumentPickerViewController(forOpeningContentTypes: [.audio], asCopy: true)
        controller.allowsMultipleSelection = false
        DocumentPickerBridge.shared.present(controller) { urls in
            completion(urls?.first)
        }
    }
}

final class DocumentPickerBridge: NSObject, UIDocumentPickerDelegate {
    static let shared = DocumentPickerBridge()
    private var completion: (([URL]?) -> Void)?

    func present(_ picker: UIDocumentPickerViewController, completion: @escaping ([URL]?) -> Void) {
        self.completion = completion
        guard let root = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene }).first?.keyWindow?.rootViewController else {
            completion(nil)
            return
        }
        picker.delegate = self
        root.present(picker, animated: true)
    }

    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        completion?(urls)
        completion = nil
    }
    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        completion?(nil)
        completion = nil
    }
}
