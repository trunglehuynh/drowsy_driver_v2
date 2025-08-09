package com.galaxylab.drowsydriver

import android.app.Application
import android.content.Context
import com.galaxylab.drowsydriver.AI.FaceDetector
import com.galaxylab.drowsydriver.Notification.LocalNotificationController
import com.galaxylab.drowsydriver.UI.CameraFragmentVM
import com.galaxylab.drowsydriver.Utility.PermissionController
import com.galaxylab.drowsydriver.Utility.REVENUE_CAT_API_KEY
import com.galaxylab.drowsydriver.Utility.UpdateController
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module
import timber.log.Timber

class MyApplication : Application() {

    private val appModel = module {
        single { this@MyApplication.getSharedPreferences("DrowsyDriver", Context.MODE_PRIVATE) }
        single {
            PermissionController(
                listOf(
                    "android.permission.CAMERA"
                ), this@MyApplication
            )
        }
    }
    private val mainActivity = module {
        single { FaceDetector() }
        single { AlertController(get(), get()) }
        viewModel {
            CameraFragmentVM(
                get(),
                get(),
                get()
            )
        }
        viewModel {
            com.galaxylab.drowsydriver.UI.SoundPicker.SoundPickerViewModel(
                this@MyApplication,
                get()
            )
        }

        single {
            val MINUTE_IN_MILLISECOND = 60000L
            val THREE_MINUTE_IN_MILLISECOND = MINUTE_IN_MILLISECOND * 3
            val HOUR_MILLISECOND = MINUTE_IN_MILLISECOND * 60

            val ONE_DAY_IN_MILLISECOND = MINUTE_IN_MILLISECOND * 60 * 24

            UpdateController(
                this@MyApplication,
                "Download this app",
                sharedPreferences = get(),
                developerGPID = "Galaxy Lab",
                feedbackEmail = "galaxylab102@gmail.com",
                appName = this@MyApplication.getString(R.string.app_name),
                askForRatingInitWaitTime = MINUTE_IN_MILLISECOND,
                askForRatingWaitTime = HOUR_MILLISECOND,
                askForShareInitWaitTime = HOUR_MILLISECOND,
                askForShareWaitTime = ONE_DAY_IN_MILLISECOND * 3
            )
        }
        single {
            UserInfo(get())
        }

    }


    private lateinit var koin: KoinApplication

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        koin = startKoin {
            androidLogger()
            androidContext(applicationContext)
            modules(appModel, mainActivity)
        }
        LocalNotificationController.createNotificationChannels(applicationContext)
        Purchases.configure(
            PurchasesConfiguration.Builder(
                context = this,
                apiKey = REVENUE_CAT_API_KEY
            ).build()
        )
    }
}
