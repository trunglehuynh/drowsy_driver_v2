package com.galaxylab.drowsydriver.Utility

// import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class General {
    companion object {
        fun FirebaseLogE(message: String) {
            Timber.e(message)
//            FirebaseCrashlytics.getInstance().log(message)

        }

        fun FirebaseLogE(throwable: Throwable) {
            Timber.e(throwable)
            throwable.message?.apply {
//                FirebaseCrashlytics.getInstance().log(this)
            }
        }
    }
}
