package com.creations.rimov.esbeta

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.util.Log
import android.util.Size
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
    var id: String? = null

    lateinit var deviceVideoDimen: Size

    var ready: Boolean = false

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
    fun open(): Boolean {
        val neededPermissions = PermissionsUtil.check(activity, PermissionsUtil.CAMERA, PermissionsUtil.AUDIO)

        if(neededPermissions.isNotEmpty()) {
            PermissionsUtil.requestVideoPermission(activity)
            ready = false
            return false
        }

        val camManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        id = CameraUtil.getFrontCameraId(camManager) ?: return false

        val char = camManager.getCameraCharacteristics(id!!)
//        char.get(CameraCharacteristics.)
        val charMap = char.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return false

        deviceVideoDimen = chooseVideoSize(charMap.getOutputSizes(MediaRecorder::class.java)) ?: return false

        Log.i("FrontCamera", "open(): chosen video dimen = (${deviceVideoDimen.width}, ${deviceVideoDimen.height})")

        try {
            camManager.openCamera(id ?: return false, stateCallback, null)
        } catch(e: NullPointerException) { //camera2API not supported
            e.printStackTrace()
        } catch(e: InterruptedException) {
            throw RuntimeException("Interrupted while locking open camera")
        }

        ready = true
        return true
    }

    fun close() {

        device?.close()
        device = null
    }

    private fun chooseVideoSize(sizes: Array<Size>) = sizes.firstOrNull { it.width <= 1080 }
}