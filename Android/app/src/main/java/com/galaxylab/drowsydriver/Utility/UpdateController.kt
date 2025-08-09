package com.galaxylab.drowsydriver.Utility


import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import com.galaxylab.drowsydriver.BuildConfig
import com.galaxylab.drowsydriver.R
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.CompletableDeferred
import timber.log.Timber


class UpdateController {
    val appUpdateInfoTask: Task<AppUpdateInfo>
    val appPackageName: String
    val appURL: String
    val appURI: String

    val developerURL: String
    val developerURI: String
    val applicationContext: Context
    val recommandTheApp: String

    private val sharedPreferences: SharedPreferences

    private val feedbackEmail: String
    private val appName: String
    private val askForRatingInitWaitTime: Long
    private val askForRatingWaitTime: Long
    private val askForShareInitWaitTime: Long
    private val askForShareWaitTime: Long
    private val THEME = R.style.Theme_AppCompat_Light_Dialog_Alert

    //private val developerGPID:String
    constructor(
        applicationContext: Context,
        RecommendTheApp: String,
        sharedPreferences: SharedPreferences,
        developerGPID: String,
        feedbackEmail: String,
        appName: String,
        askForRatingInitWaitTime: Long,
        askForRatingWaitTime: Long,
        askForShareInitWaitTime: Long,
        askForShareWaitTime: Long
    ) {
        this.askForRatingInitWaitTime = askForRatingInitWaitTime
        this.askForRatingWaitTime = askForRatingWaitTime
        this.askForShareInitWaitTime = askForShareInitWaitTime
        this.askForShareWaitTime = askForShareWaitTime

        this.feedbackEmail = feedbackEmail
        this.appName = appName
        this.sharedPreferences = sharedPreferences

        this.recommandTheApp = RecommendTheApp
        this.applicationContext = applicationContext
        val appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
        appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appPackageName =
            applicationContext.packageName// getPackageName() from Context or Activity object
        appURL = "https://play.google.com/store/apps/details?id=$appPackageName"
        appURI = "market://details?id=$appPackageName"

        developerURI = "market://search?q=pub:$developerGPID"
        developerURL =
            "https://play.google.com/store/apps/developer?id=${developerGPID.replace(' ', '+')}"

    }

    suspend fun isNeedUpdate(): AppUpdateInfo? {

        val response = CompletableDeferred<AppUpdateInfo?>()
        Timber.d("isNeedUpdate")
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                // Request the update.
                response.complete(appUpdateInfo)
            } else
                response.complete(null)
            Timber.d("appUpdateInfo $appUpdateInfo")
        }

        return response.await()
    }

    fun askForUpdate(activity: Activity) {

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                // Request the update.
                AlertDialog.Builder(activity, THEME).setTitle("Update")
                    .setMessage("A new version is available.")
                    .setPositiveButton("Update") { _, _ ->
                        openAppOnGooglePlay(activity)
                    }
                    .create()
                    .show()
            }
        }
    }

    fun openAppOnGooglePlay(activity: Activity) {

        sendGooglePlay(activity, uri = appURI, url = appURL)
    }

    private fun sendGooglePlay(context: Context, uri: String, url: String) {
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(uri)
                )
            )
        } catch (e: Exception) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )
            )
        }
    }

    fun openAllAppOnGooglePlay(activity: Activity) {
        sendGooglePlay(activity, uri = developerURI, url = developerURL)
    }

    fun opeAppOnGooglePlay(activity: Activity, appId: String) {
        val appURL = "https://play.google.com/store/apps/details?id=$appId"
        val appURI = "market://details?id=$appId"
        sendGooglePlay(activity, appURI, appURL)
    }

    fun openApp(context: Context, appId: String): Boolean {

        Timber.d("open app $appId")
        val manager = context.packageManager
        try {
            val intent = manager.getLaunchIntentForPackage(appId)
            //throw new ActivityNotFoundException();
            intent?.apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                context.startActivity(this)
                return true
            }
            return false
        } catch (e: ActivityNotFoundException) {
            Timber.e(e)
            return false
        }
    }


    fun shareApp(activity: Activity?) {

        activity?.apply {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    applicationContext.getString(R.string.app_name)
                )
                val shareMessage =
                    "$recommandTheApp\n\n $appURL"
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                startActivity(Intent.createChooser(shareIntent, "choose one"))
            } catch (e: java.lang.Exception) {
                Timber.e(e)
//                Crashlytics.logException(e)
            }
        }
    }

    private val RATING_WAITING_TIME = "RATING_WAITING_TIME"
//    private val DAY_IN_MILLISECOND: Long = 86400000L

    fun askForRating(activity: Activity) {
        val currentTime = System.currentTimeMillis()
        val lastAsk = sharedPreferences.getLong(RATING_WAITING_TIME, -1L)

        // the first time
        if (lastAsk == -1L) {
            sharedPreferences.edit()
                .putLong(
                    RATING_WAITING_TIME,
                    currentTime + askForRatingInitWaitTime
                ).apply()
            return
        }

        if (lastAsk <= currentTime) {
            AlertDialog.Builder(activity, THEME).setPositiveButton("Yes") { _, _ ->
                // never ask one again
                sharedPreferences.edit().putLong(RATING_WAITING_TIME, Long.MAX_VALUE).commit()
                openAppOnGooglePlay(activity)
            }.setNegativeButton("No") { _, _ ->
                sharedPreferences.edit()
                    .putLong(
                        RATING_WAITING_TIME,
                        currentTime + askForRatingWaitTime
                    ).apply()
                askForFeedBack(activity)
            }.setTitle("Rating")
                .setMessage("Do you like this app?")
                .create().show()
        }

    }

    private fun askForFeedBack(activity: Activity) {
        AlertDialog.Builder(activity, THEME).setPositiveButton("Yes") { _, _ ->
            sendFeedBack(activity)
        }
            .setTitle("FeedBack")
            .setMessage("Do you want to send an email feedback to developer?")
            .create().show()
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun sendFeedBack(activity: Activity) {

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(feedbackEmail))
            putExtra(
                Intent.EXTRA_SUBJECT,
                "[Android][$appName][${BuildConfig.VERSION_CODE} vs ${BuildConfig.VERSION_NAME}]: Question & Feedback"
            )
            data = Uri.parse("mailto:") // Only email apps should handle this
        }
        activity.startActivity(intent)
    }

    private val SHARE_WAITING_TIME = "SHARE_WAITING_TIME"

    fun askForSharing(activity: Activity) {

        val currentTime = System.currentTimeMillis()
        val lastAsk = sharedPreferences.getLong(SHARE_WAITING_TIME, -1L)

        // the first time
        if (lastAsk == -1L) {
            sharedPreferences.edit()
                .putLong(
                    SHARE_WAITING_TIME,
                    currentTime + askForShareInitWaitTime
                ).apply()
            return
        }

        if (lastAsk <= currentTime) {

            sharedPreferences.edit()
                .putLong(
                    SHARE_WAITING_TIME,
                    currentTime + askForShareWaitTime
                ).apply()

            AlertDialog.Builder(activity, THEME)
                .setTitle("Share App")
                .setMessage("Do you want to share this app with your friends?")

                .setPositiveButton("Yes") { _, _ ->
                    shareApp(activity)
                }.create().show()
        }

    }

}