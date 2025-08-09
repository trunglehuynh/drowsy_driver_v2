package com.galaxylab.drowsydriver.UI

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.galaxylab.drowsydriver.BuildConfig
import com.galaxylab.drowsydriver.Notification.LocalNotificationController
import com.galaxylab.drowsydriver.R
import com.galaxylab.drowsydriver.UI.Disclaimer.DisclaimerFragment
import com.galaxylab.drowsydriver.UserInfo
import com.galaxylab.drowsydriver.Utility.PermissionController
import com.galaxylab.drowsydriver.Utility._SceenAllwayOn
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLauncher
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResultHandler
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private val permissionController: PermissionController = get()
    private val userInfo: UserInfo = get()
    private val TAG = "MainActivity"
    private val cameraFragmentVM: CameraFragmentVM by viewModel()
    private lateinit var paywallActivityLauncher: PaywallActivityLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initFirebaseConfig()
        paywallActivityLauncher = PaywallActivityLauncher(this, object : PaywallResultHandler {
            override fun onActivityResult(result: PaywallResult) {
                Timber.d(result.toString())
            }
        })

        showNextFragment()

        this._SceenAllwayOn()
        mayLogNotificationReminderEvent()

        supportFragmentManager.addOnBackStackChangedListener {
            supportFragmentManager.let {
                val mainFragment = it.findFragmentById(R.id.fragmentContainer) as? MainFragment
                mainFragment?.initShortcutBtn()
            }
        }
        userInfo.mayUpdateInitialInstallVersion()
//        Timber.d("getInitialInstallVersion : ${userInfo.getInitialInstallVersion()}")
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        Timber.d("onPictureInPictureModeChanged orientation , ${newConfig.orientation} isInPictureInPictureMode $isInPictureInPictureMode")
        cameraFragmentVM.setPiPMode(isInPictureInPictureMode)
    }

    fun showNextFragment() {
        if (!userInfo.isUserAgreedDisclaimer()) {
            //show disclaimer fragment
            popBackToRoot()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, DisclaimerFragment())
                .addToBackStack(null)
                .commit()
        } else if (!permissionController.isAllPermissionGranted()) {
            //show permission fragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PermissionFragment())
                .addToBackStack(null)
                .commit()

        } else {
            showMainFragment()
//            showCameraFragment()
        }
    }


    private fun initFirebaseConfig() {
        try {
            val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 1000 else 3600
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
            remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
            remoteConfig.fetchAndActivate().addOnCompleteListener {
                if (it.isSuccessful) {
                    Timber.d("$TAG , addOnCompleteListener result ${it.result}")
                } else {
                    Firebase.crashlytics.recordException(Exception("remoteConfig load Unsuccessfully"))
                }
            }.addOnFailureListener { Timber.e("$TAG , addOnFailureListener result $it") }
                .addOnCanceledListener { Timber.e("$TAG , addOnCanceledListener ") }
        } catch (ex: Exception) {
            Timber.e(ex)
            Firebase.crashlytics.recordException(ex)
        }
    }

    private fun mayLogNotificationReminderEvent() {
        val requestCode = intent.getIntExtra(LocalNotificationController.KEY_NOTIFICATION_ID, -1)
        if (requestCode == LocalNotificationController.REMINDER_NOTIFICATION_ID) {
            Firebase.analytics.logEvent("send_notification_reminder_open", null)
        }
    }

//    fun showOptionFragment() {
//        popBackToRoot()
//        supportFragmentManager.beginTransaction()
//            .replace(R.id.fragmentContainer, OptionFragment())
//            .commit()
//    }

    fun showMainFragment() {
        popBackToRoot()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, MainFragment())
            .commit()
    }

    fun mayOpenCameraFragment() {
        // free for previous users
        if (!userInfo.shouldShowPaywallForPreviousUsers()  || BuildConfig.DEBUG
            ) {
            openCameraFragment()
            return
        }
        Purchases.sharedInstance.getCustomerInfoWith(
            onError = { error ->
                Timber.tag("RevenueCat").e("Failed to fetch customer info: $error")
                val bundle = Bundle().apply {
                    putString("error_message", error.message)
                    putString("error_code", error.code.name)
                }
                Firebase.analytics.logEvent("revenue_cat_get_customer_info_error", bundle)
                Toast.makeText(this,"Sync failed. Check your internet connection.",Toast.LENGTH_LONG).show()
            },
            onSuccess = { customerInfo ->
                val isPro =
                    customerInfo.entitlements["premium version entitlement"]?.isActive == true
                if (isPro) {
                    // User has active subscription
                    openCameraFragment()
                    Timber.d("pro version ")
                    return@getCustomerInfoWith
                }

                // User is not subscribed — fetch offering and launch paywall
                Purchases.sharedInstance.getOfferingsWith(
                    onError = { error ->
                        Timber.tag("RevenueCat").e("Error fetching offerings: $error")
                        val bundle = Bundle().apply {
                            putString("error_message", error.message)
                            putString("error_code", error.code.name)
                        }
                        Firebase.analytics.logEvent("revenue_cat_get_offers_error", bundle)
                        Toast.makeText(this,"Sync failed. Check your internet connection.",Toast.LENGTH_LONG).show()
                    },
                    onSuccess = { offerings ->
                        val currentOffering = offerings.current
                        Timber.tag("RevenueCat").d("offerings $currentOffering")
                        if (currentOffering == null) {
                            Timber.tag("RevenueCat").d("No current offering available")
                            Firebase.analytics.logEvent(
                                "revenue_cat_current_offering_null",
                                null
                            )
                            return@getOfferingsWith
                        }
                        Firebase.analytics.logEvent("revenue_cat_get_show_pay_wall", null)
                        paywallActivityLauncher.launchIfNeeded(
                            offering = currentOffering,
                            fontProvider = null,
                            shouldDisplayDismissButton = true,
                            edgeToEdge = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM),
                            shouldDisplayBlock = { true }
                        )
                    }
                )
            }
        )
    }

    private fun openCameraFragment() {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, CameraFragment())
            .addToBackStack(null)
            .commit()
    }

    fun popBackToRoot() {
        supportFragmentManager.popBackStack(
            null,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
    }
}