package com.galaxylab.drowsydriver.UI

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.galaxylab.drowsydriver.Notification.LocalNotificationController
import com.galaxylab.drowsydriver.R
import com.galaxylab.drowsydriver.UserInfo
import com.galaxylab.drowsydriver.Utility.UpdateController
import com.galaxylab.drowsydriver.databinding.FragmentMainBinding
import org.koin.android.ext.android.get
import timber.log.Timber


class MainFragment : Fragment() {

    private val sharedPreferences: SharedPreferences = get()
    private val updateController: UpdateController = get()
    private val userInfo: UserInfo = get()
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.menuBtn.setOnClickListener { showPopup(it) }

        binding.startBtn.setOnClickListener { (context as MainActivity).mayOpenCameraFragment() }

        binding.advanceOptionBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, OptionFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.addShortcutBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, AddAddressShortcutFragment())
                .addToBackStack(null)
                .commit()
        }
        initShortcutBtn()
        LocalNotificationController.mayScheduleNotificationOrAskForPermission(this)

        requireActivity().supportFragmentManager.addOnBackStackChangedListener {
            fun onBackStackChanged() {
                val fragment =
                    requireActivity().supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                Timber.d("onBackStackChanged $fragment")
                if (fragment == this) {
                    initShortcutBtn()
                }
            }
        }
    }

//    private fun openCameraFragment() {
//        parentFragmentManager.beginTransaction()
//            .add(R.id.fragmentContainer, CameraFragment())
//            .addToBackStack(null)
//            .commit()
//    }

//    private fun mayOpenCameraFragment() {
//        // free for previous users
//        if (!userInfo.shouldShowPaywallForPreviousUsers()){
//            openCameraFragment()
//            return
//        }
//        Purchases.sharedInstance.getCustomerInfoWith(
//            onError = { error ->
//                Timber.tag("RevenueCat").e("Failed to fetch customer info: $error")
//                val bundle = Bundle().apply {
//                    putString("error_message", error.message)
//                    putString("error_code", error.code.name)
//                }
//                Firebase.analytics.logEvent("revenue_cat_get_customer_info_error", bundle)
//            },
//            onSuccess = { customerInfo ->
//                val isPro =
//                    customerInfo.entitlements["premium version entitlement"]?.isActive == true
//                if (isPro) {
//                    // User has active subscription
//                    openCameraFragment()
//                    Timber.d("pro version ")
//                    return@getCustomerInfoWith
//                }
//
//                // User is not subscribed — fetch offering and launch paywall
//                Purchases.sharedInstance.getOfferingsWith(
//                    onError = { error ->
//                        Timber.tag("RevenueCat").e("Error fetching offerings: $error")
//                        val bundle = Bundle().apply {
//                            putString("error_message", error.message)
//                            putString("error_code", error.code.name)
//                        }
//                        Firebase.analytics.logEvent("revenue_cat_get_offers_error", bundle)
//                    },
//                    onSuccess = { offerings ->
//                        val currentOffering = offerings.current
//                        Timber.tag("RevenueCat").d("offerings $currentOffering")
//                        if (currentOffering == null) {
//                            Timber.tag("RevenueCat").d("No current offering available")
//                            Firebase.analytics.logEvent(
//                                "revenue_cat_current_offering_null",
//                                null
//                            )
//                            return@getOfferingsWith
//                        }
//                        Firebase.analytics.logEvent("revenue_cat_get_show_pay_wall", null)
//                        paywallActivityLauncher.launchIfNeeded(
//                            offering = currentOffering,
//                            fontProvider = null,
//                            shouldDisplayDismissButton = true,
//                            edgeToEdge = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM),
//                            shouldDisplayBlock = { true }
//                        )
//                    }
//                )
//            }
//        )
//    }

    fun initShortcutBtn() {
        Timber.d("initShortcutBtn")
        val address = sharedPreferences.getString(
            AddAddressShortcutFragment.SAVE_DESTINATION_ADDRESS_KEY,
            null
        )
        if (address.isNullOrBlank()) {
            binding.shortcutBtn.visibility = View.GONE
            return
        }

        binding.shortcutBtn.text = address
        binding.shortcutBtn.visibility = View.VISIBLE
        binding.shortcutBtn.setOnClickListener {
            val cameraFragment = CameraFragment()
            val args = Bundle().apply {
                putString(CameraFragment.DESTINATION_ADDRESS_KEY, address)
            }
            cameraFragment.arguments = args
            parentFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, cameraFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.apply {
            updateController.askForRating(this)
            updateController.askForSharing(this)
            updateController.askForUpdate(this)
        }
    }

    private fun showPopup(v: View?) {
        val popup = PopupMenu(context, v)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.main_menu, popup.menu)
        popup.setOnMenuItemClickListener {

            when (it.itemId) {

                R.id.menuUpdate -> {
                    updateController.openAppOnGooglePlay(requireActivity())
                    true
                }

                R.id.menuRate -> {
                    updateController.openAppOnGooglePlay(requireActivity())

                    true
                }

                R.id.menuShare -> {
                    updateController.shareApp(requireActivity())
                    true
                }

                R.id.menuMoreApp -> {
                    updateController.openAllAppOnGooglePlay(requireActivity())
                    true
                }

                R.id.menuFeedback -> {
                    updateController.sendFeedBack(requireActivity())
                    true
                }
            }
            false
        }
        popup.show()
    }
}