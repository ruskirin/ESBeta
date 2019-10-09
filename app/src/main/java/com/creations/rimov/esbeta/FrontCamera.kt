package com.creations.rimov.esbeta

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.widget.Toast
import com.creations.rimov.esbeta.util.CameraUtil
import com.creations.rimov.esbeta.util.PermissionsUtil
import java.lang.NullPointerException
import java.lang.RuntimeException

/**
 * Used the following for guidance:
 *   https://github.com/android/camera-samples/blob/master/Camera2VideoKotlin/Application/src/main/java/com/example/android/camera2video/Camera2VideoFragment.kt
 */
class FrontCamera(private val cameraActivity: Activity) {

    private var bgThread: HandlerThread? = null
    private var bgHandler: Handler? = null

    private var captureSession: CameraCaptureSession? = null
    private lateinit var previewRequestBuilder: CaptureRequest.Builder

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
            cameraActivity.finish()
        }
    }

    @SuppressLint("MissingPermission")
    fun open() {

        if(!PermissionsUtil.haveVideoPermission(cameraActivity)) {
            Log.i("FrontCamera", "open(): don't have required permissions!")
            PermissionsUtil.requestVideoPermission(cameraActivity)
            return
        }

        Log.i("FrontCamera", "open(): permissions obtained.")

        val manager = cameraActivity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            id = CameraUtil.getFrontCameraId(manager)
            char = manager.getCameraCharacteristics(id ?: "1")

            manager.openCamera(id!!, stateCallback, null)

        } catch(e: CameraAccessException) {
            Toast.makeText(cameraActivity, "Cannot open camera", Toast.LENGTH_SHORT).show()
            cameraActivity.finish()

        } catch(e: NullPointerException) { //camera2 is used but not supported
            Toast.makeText(cameraActivity, "Device not compatible with used API (camera2)", Toast.LENGTH_SHORT).show()
        }
    }

    fun close() {

        try {
            closePreviewSession()
            device?.close()
            device = null

        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        }
    }

    fun setUpCaptureSession(surfaces: ArrayList<Surface>, recorder: MediaRecorder?) {

        previewRequestBuilder = device!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            addTarget(surfaces[0])
        }

        device?.let {
            it.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(surfaces[0]) //First element is the MediaRecorder surface
            }

            it.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        captureSession = cameraCaptureSession
                        updatePreview()
                        cameraActivity.runOnUiThread { recorder?.start() }
                    }
                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {}
            }, bgHandler)
        }
    }

    /**
     * Starts a background thread and its [Handler].
     */
    fun startBgThread() {

        bgThread = HandlerThread("CameraBackground").apply {
            start()
            bgHandler = Handler(this.looper)
        }
//        bgHandler = Handler(bgThread?.looper)
    }

    /**
     * Stops the background thread and its [Handler].
     */
    fun stopBgThread() {

        bgThread?.quitSafely()
        try {
            bgThread?.join()
            bgThread = null
            bgHandler = null

        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun getLargestSize(): Size? {
        val configMap = char?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: throw RuntimeException("Cannot retrieve video size")

        return configMap.getOutputSizes(MediaRecorder::class.java).firstOrNull {
            it.width <= 1080
        }
    }

    private fun setUpCaptureRequestBuilder(builder: CaptureRequest.Builder?) {
        builder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
    }

    private fun updatePreview() {
        if (device  == null) return

        try {
            setUpCaptureRequestBuilder(previewRequestBuilder)
            HandlerThread("CameraPreview").start()
            captureSession?.setRepeatingRequest(previewRequestBuilder.build(),
                null, bgHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun closePreviewSession() {
        captureSession?.close()
        captureSession = null
    }
}