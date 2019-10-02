package com.creations.rimov.esbeta

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.util.Log
import android.util.Size
import android.widget.Toast
import com.creations.rimov.esbeta.util.CameraUtil
import com.creations.rimov.esbeta.util.PermissionsUtil
import java.lang.NullPointerException
import java.lang.RuntimeException

/**
 * Used the following for guidance:
 *   https://github.com/android/camera-samples/blob/master/Camera2VideoKotlin/Application/src/main/java/com/example/android/camera2video/Camera2VideoFragment.kt
 */
class FrontCamera(private val activity: Activity) {

    var device: CameraDevice? = null
    var id: String? = null
    var char: CameraCharacteristics? = null

    //A callback object for receiving updates about the state of a camera device.
    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cam: CameraDevice) {
            device = cam
        }
        //Camera no longer available for use
        override fun onDisconnected(cam: CameraDevice) {
            device?.close()
            device = null
        }

        override fun onError(cam: CameraDevice, error: Int) {
            device?.close()
            device = null
            activity.finish()
        }
    }

    @SuppressLint("MissingPermission")
    fun open(): Boolean {

        if(!PermissionsUtil.haveVideoPermission(activity)) {
            Log.i("FrontCamera", "open(): don't have required permissions!")
            PermissionsUtil.requestVideoPermission(activity)
            return false
        }

        Log.i("FrontCamera", "open(): permissions obtained.")

        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            id = CameraUtil.getFrontCameraId(manager)
            char = manager.getCameraCharacteristics(id ?: return false)

            manager.openCamera(id!!, stateCallback, null)
            return true

        } catch(e: CameraAccessException) {
            Toast.makeText(activity, "Cannot open camera", Toast.LENGTH_SHORT).show()
            activity.finish()
            return false

        } catch(e: NullPointerException) { //camera2 is used but not supported
            Toast.makeText(activity, "Device not compatible with used API (camera2)", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    fun close() {

        device?.close()
        device = null
    }

    fun getVideoSize(): Size? {
        val configMap = char?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: throw RuntimeException("Cannot retrieve video size")

        return configMap.getOutputSizes(MediaRecorder::class.java).firstOrNull { it.width <= 1080 }
    }
}