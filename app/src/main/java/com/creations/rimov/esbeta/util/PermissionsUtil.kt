package com.creations.rimov.esbeta.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.creations.rimov.esbeta.FrontCamera

object PermissionsUtil {

    const val CAMERA = 3001
    const val AUDIO = 3002
    const val STORAGE_EXT = 4001

    //Has @param{permission} already been granted?
    @JvmStatic
    fun check(context: Context, vararg permission: Int): List<Int> {
        val types = arrayListOf<Int>()

        permission.forEach { permission ->
            val type: String = when (permission) {
                CAMERA -> Manifest.permission.CAMERA
                AUDIO -> Manifest.permission.RECORD_AUDIO
                STORAGE_EXT -> Manifest.permission.WRITE_EXTERNAL_STORAGE
                else -> return@forEach
            }

            if((ContextCompat.checkSelfPermission(context, type) != PackageManager.PERMISSION_GRANTED))
                types.add(permission)
        }

        return types
    }

    //If rationale for the @param{permission} is needed, display @param{message}
    @JvmStatic
    fun showRationale(activity: Activity, permission: Int, message: String = "Permission required for app functionality") {
        val type: String? = when (permission) {
            CAMERA -> Manifest.permission.CAMERA
            AUDIO -> Manifest.permission.RECORD_AUDIO
            STORAGE_EXT -> Manifest.permission.WRITE_EXTERNAL_STORAGE
            else -> null
        }

        //Explain why you need the permission
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                activity, type ?: return)) {
            Toast.makeText(activity.applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    //NOTE: Since SDK 23(24?), permission must be requested at runtime if it has not already been granted
    //Request @param{permissions}, supplying personal @param{requestCode} for the "transaction"
    @JvmStatic
    fun requestPermission(activity: Activity, requestCode: Int, vararg permissions: Int) {
        val types: ArrayList<String> = arrayListOf()

        permissions.forEach { permission ->
            when(permission) {
                CAMERA -> types.add(Manifest.permission.CAMERA)
                AUDIO -> types.add(Manifest.permission.RECORD_AUDIO)
                STORAGE_EXT -> types.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        ActivityCompat.requestPermissions(activity, types.toTypedArray(), requestCode)
    }

    @JvmStatic
    fun requestVideoPermission(activity: Activity) {

        showRationale(activity, CAMERA)
        showRationale(activity, AUDIO)
        showRationale(activity, STORAGE_EXT)

        requestPermission(activity, FrontCamera.Constant.VIDEO_REQUEST_CODE,
            CAMERA, AUDIO, STORAGE_EXT)
    }
}