package com.creations.rimov.esbeta.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionsUtil {

    const val VIDEO_REQUEST_CODE = 9000

    val VIDEO_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @JvmStatic
    fun havePermission(context: Context, vararg permission: String) = permission.none { perm ->
            ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
    }

    @JvmStatic
    fun showRationale(activity: Activity,
                      permission: String,
                      message: String = "Permission required for app functionality") {

        //Explain why you need the permission
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                activity, permission)) {

            Toast.makeText(activity.applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    @JvmStatic
    fun haveVideoPermission(activity: Activity) = VIDEO_PERMISSIONS.none {
        !havePermission(activity, it)
    }

    @JvmStatic
    fun requestVideoPermission(activity: Activity) {
        val neededPerms = arrayListOf<String>()

        VIDEO_PERMISSIONS.forEach {
            if(!havePermission(activity, it)) {
                neededPerms.add(it)
                showRationale(activity, it)
            }
        }

        ActivityCompat.requestPermissions(activity, neededPerms.toTypedArray(), VIDEO_REQUEST_CODE)
    }
}