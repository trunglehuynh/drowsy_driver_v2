package com.galaxylab.drowsydriver.Notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationID = intent.getIntExtra(LocalNotificationController.KEY_NOTIFICATION_ID, 0)
        if (notificationID == LocalNotificationController.SCHEDULE_NOTIFICATION_REQUEST_CODE) {
            LocalNotificationController.sendReminderNotification(context.applicationContext)
        }
    }
}