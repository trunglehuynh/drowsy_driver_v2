package com.galaxylab.drowsydriver.Utility

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import com.galaxylab.drowsydriver.R
import timber.log.Timber

class PermissionController {

    private val permissionList: List<String>
    private val applicationContext: Context
    private val permissionCode = 12345

    constructor(permissionList: List<String>, context: Context) {
        this.permissionList = permissionList
        this.applicationContext = context
    }

    fun isAllPermissionGranted(): Boolean {
        return permissionList.all { isGrantedPermission(applicationContext, it) }
    }

    fun askAllNotGrantedPermissions(activity: Activity) {
        val notGrantedPermissionList =
            permissionList.filter { !isGrantedPermission(activity, it) }.toList()
        if (notGrantedPermissionList.isEmpty()) return
        requestPermissions(activity, notGrantedPermissionList.toTypedArray(), permissionCode)

    }


    fun isGrantedPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun askPermissionIfNotGranted(
        activity: Activity, permissions: Array<String>, title: String, body: String,
        positiveButton: String = "Ok", negativeButton: String = "cancel",
        theme: Int = R.style.Theme_AppCompat_Light_Dialog_Alert,
        positiveAction: (activity: Activity) -> Unit = ::openAppSetting,
        negativeAction: () -> Unit = {}
    ): Boolean {
        Timber.d("askPermissionIfNotGranted $permissions")
        if (permissions.all { isGrantedPermission(activity, it) }) return false

        openDialog(
            title, body, positiveButton, negativeButton,
            theme, activity, positiveAction, negativeAction
        )
        return true
    }

    private fun openDialog(
        title: String,
        body: String,
        positiveButton: String = "Ok",
        negativeButton: String = "cancel",
        theme: Int = R.style.Theme_AppCompat_Light_Dialog_Alert,
        activity: Activity,
        positiveAction: (activity: Activity) -> Unit,
        negativeAction: () -> Unit = {}
    ) {
        val dialog = AlertDialog.Builder(activity, theme)


        val alertDialog =
            dialog.setPositiveButton(positiveButton) { _, _ -> positiveAction(activity) }
                .setNegativeButton(negativeButton) { _, _ -> negativeAction() }
                .setTitle(title)
                .setMessage(body).create()

        dialog.show()


    }

    fun openAppSetting(activity: Activity) {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        activity.startActivityForResult(intent, permissionCode)
        Timber.d("openAppSetting")
    }
}

interface PermissionResult {
    fun result(grantResults: IntArray)
}