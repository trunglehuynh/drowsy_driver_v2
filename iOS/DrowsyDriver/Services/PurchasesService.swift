import Foundation
#if canImport(RevenueCat)
import RevenueCat
#endif

final class PurchasesService: ObservableObject {
    enum EntitlementStatus { case active, inactive }
    @Published private(set) var status: EntitlementStatus = .inactive

    func configure(apiKey: String) {
        #if canImport(RevenueCat)
        Purchases.configure(withAPIKey: apiKey)
        refresh()
        #endif
    }

    func refresh() {
        #if canImport(RevenueCat)
        Purchases.shared.getCustomerInfo { [weak self] info, _ in
            let active = info?.entitlements.active.values.isEmpty == false
            DispatchQueue.main.async { self?.status = active ? .active : .inactive }
        }
        #endif
    }

    func purchase(presentation: UIViewController?, completion: @escaping (Bool) -> Void) {
        #if canImport(RevenueCat)
        Purchases.shared.getOfferings { offerings, error in
            guard let package = offerings?.current?.availablePackages.first, error == nil else {
                completion(false); return
            }
            Purchases.shared.purchase(package: package) { _, info, _, _ in
                let active = info?.entitlements.active.values.isEmpty == false
                completion(active)
            }
        }
        #else
        completion(false)
        #endif
    }

    func restore(completion: @escaping (Bool) -> Void) {
        #if canImport(RevenueCat)
        Purchases.shared.restorePurchases { info, _ in
            let active = info?.entitlements.active.values.isEmpty == false
            completion(active)
        }
        #else
        completion(false)
        #endif
    }
}

