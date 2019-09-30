package com.creations.rimov.esbeta

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import com.creations.rimov.esbeta.util.CameraUtil
import com.creations.rimov.esbeta.util.PermissionsUtil
import java.lang.NullPointerException
import java.lang.RuntimeException


/**
 * Used the following for guidance:
 *   https://github.com/android/camera-samples/blob/master/Camera2VideoKotlin/Application/src/main/java/com/example/android/camera2video/Camera2VideoFragment.kt
 */
class FrontCamera(private val activity: Activity) {

    object Constant {
        const val VIDEO_REQUEST_CODE = 9000
    }

    var device: CameraDevice? = null

    private var id: String? = null

    //A callback object for receiving updates about the state of a camera device.
    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cam: CameraDevice) {
            device = cam
        }
        //Camera no longer available for use
        override fun onDisconnected(cam: CameraDevice) {
            cam.close()
            device = null
        }

        override fun onError(cam: CameraDevice, error: Int) {
            cam.close()
            device = null
            activity.finish()
        }
    }

    @SuppressLint("MissingPermission")
    fun open() {
        val neededPermissions = PermissionsUtil.check(activity, PermissionsUtil.CAMERA, PermissionsUtil.AUDIO)

        if(neededPermissions.isNotEmpty()) {
            requestVideoPermission()
        }

        val camManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        id = CameraUtil.getFrontCameraId(camManager) ?: return

        try {
            camManager.openCamera(id ?: return, stateCallback, null)
        } catch(e: NullPointerException) { //camera2API not supported
            e.printStackTrace()
        } catch(e: InterruptedException) {
            throw RuntimeException("Interrupted while locking open camera")
        }
    }

    fun close() {

        device?.close()
        device = null
    }

    private fun requestVideoPermission() {

        PermissionsUtil.showRationale(activity, PermissionsUtil.CAMERA)
        PermissionsUtil.showRationale(activity, PermissionsUtil.AUDIO)

        PermissionsUtil.requestPermission(activity, Constant.VIDEO_REQUEST_CODE, PermissionsUtil.CAMERA, PermissionsUtil.AUDIO)
    }
}