import SwiftUI

struct DisclaimerView: View {
    var onAgree: () -> Void
    var body: some View {
        VStack(spacing: 16) {
            Text("Disclaimer").font(.title2).bold()
            Text("This app assists by detecting signs of drowsiness but does not replace attentive driving. Always keep your eyes on the road and hands on the wheel.")
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            Button(action: onAgree) {
                Text("I Understand and Agree")
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Color.accentColor)
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
            }
        }
        .padding()
    }
}

