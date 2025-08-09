package com.galaxylab.drowsydriver.Notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.galaxylab.drowsydriver.BuildConfig
import com.galaxylab.drowsydriver.R
import com.galaxylab.drowsydriver.UI.MainActivity
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.remoteconfig.remoteConfig
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId


class LocalNotificationController {

    companion object {
        const val REMINDER_NOTIFICATION_ID = 1001
        const val SCHEDULE_NOTIFICATION_REQUEST_CODE = 1111
        const val KEY_NOTIFICATION_ID = "NOTIFICATION_ID"
        const val REMINDER_NOTIFICATION_CHANNEL_ID = "REMINDER_NOTIFICATION_CHANNEL_ID"
        const val MILLISECOND_ONE_DAY = 86400000L
        const val MILLISECOND_IN_5_SECOND = 5000L

        /**
         *
         * 1) from Option Fragment -> send an alarm to NotificationReceiver every 5pm
         * 2) NotificationReceiver show a notification only
         * 3) open Option Fragment -> repeat step 1 with update/override
         * note: only need 1 ID for notification and 1 ID for requestCode
         *
         * **/

        fun scheduleNotification(context: Context) {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra(KEY_NOTIFICATION_ID, SCHEDULE_NOTIFICATION_REQUEST_CODE)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                SCHEDULE_NOTIFICATION_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val isDebugging = BuildConfig.DEBUG
            val delayTime = MILLISECOND_ONE_DAY // 1 day from now
            val triggerAtMillis = System.currentTimeMillis() + delayTime

            val repeatTime = MILLISECOND_ONE_DAY
            val time5PM = if (isDebugging) triggerAtMillis else convertTimeToLocalTimeAndSet(
                triggerAtMillis, 17 // 5pm
            )

            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP, time5PM, repeatTime, pendingIntent
            )

        }

        private fun convertTimeToLocalTimeAndSet(timestamp: Long, hour24Format: Int): Long {
            val instant = Instant.ofEpochSecond(timestamp)
            val zoneId = ZoneId.systemDefault()
            val localDateTime =
                LocalDateTime.ofInstant(instant, zoneId).withHour(hour24Format).withMinute(0)
                    .withSecond(0)
                    .withNano(0)
            val offset = zoneId.rules.getOffset(localDateTime)

            return localDateTime.toEpochSecond(offset) * 1000
        }


        fun sendReminderNotification(context: Context) {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.areNotificationsEnabled()
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED || !notificationManager.areNotificationsEnabled()
            ) {
                return
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra(KEY_NOTIFICATION_ID, REMINDER_NOTIFICATION_ID)
            }
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

            val pendingIntent = PendingIntent.getActivity(
                context,
                REMINDER_NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notificationBuilder =
                NotificationCompat.Builder(context, REMINDER_NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(context.getString(R.string.Make_sure_you_are_not_falling_asleep))
                    .setSmallIcon(R.drawable.baseline_directions_car_24)
                    .setContentIntent(pendingIntent)

            notificationManager.notify(REMINDER_NOTIFICATION_ID, notificationBuilder.build())
            Firebase.analytics.logEvent("send_notification_reminder", null)
        }

        fun mayScheduleNotificationOrAskForPermission(fragment: Fragment) {
            if (!Firebase.remoteConfig.getBoolean("is_reminder_notification_enabled")) {
                return
            }
            val isNotificationPermissionGranted =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(
                    fragment.requireContext(), Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            if (isNotificationPermissionGranted) {
                scheduleNotification(fragment.requireContext())
                return
            }
            val requestPermissionLauncher =
                fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                    if (isGranted) {
                        scheduleNotification(fragment.requireContext())
                    }
                }
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }


        fun createNotificationChannels(applicationContext: Context) {

            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val remindersNotificationChannel = NotificationChannel(
                REMINDER_NOTIFICATION_CHANNEL_ID,
                "Reminder channel",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "The channel shows reminder notifications" }
            notificationManager.createNotificationChannel(remindersNotificationChannel)
        }
    }


}