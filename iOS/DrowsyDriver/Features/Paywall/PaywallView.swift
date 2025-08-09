import SwiftUI
import UIKit

struct PaywallView: View {
    @ObservedObject var purchases: PurchasesService
    var onDone: (Bool) -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text("Drowsy Driver Pro").font(.title2).bold()
            Text("Unlock full features and future updates.")
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            HStack(spacing: 12) {
                Button(action: restore) {
                    Text("Restore").frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)

                Button(action: subscribe) {
                    Text("Subscribe").frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
            }
            .padding(.horizontal)
        }
        .padding()
    }

    private func subscribe() {
        let root = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first?
            .windows
            .first { $0.isKeyWindow }?
            .rootViewController
        purchases.purchase(presentation: root) { success in onDone(success) }
    }
    private func restore() {
        purchases.restore { success in onDone(success) }
    }
}
