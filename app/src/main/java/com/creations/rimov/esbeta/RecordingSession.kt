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
import android.util.SparseIntArray
import android.view.Surface
import android.widget.Toast
import com.creations.rimov.esbeta.extensions.stdPattern
import com.creations.rimov.esbeta.util.CameraUtil
import com.creations.rimov.esbeta.util.PermissionsUtil
import java.lang.NullPointerException
import java.lang.RuntimeException
import java.util.*

/**
 * Used the following for guidance:
 *   https://github.com/android/camera-samples/blob/master/Camera2VideoKotlin/Application/src/main/java/com/example/android/camera2video/Camera2VideoFragment.kt
 */
class RecordingSession(private val recordActivity: Activity) {

    object ScreenOrientations {
        val NORMAL = SparseIntArray().apply {
            append(Surface.ROTATION_0, 90)
            append(Surface.ROTATION_90, 0)
            append(Surface.ROTATION_180, 270)
            append(Surface.ROTATION_270, 180)
        }
        val INVERSE = SparseIntArray().apply {
            append(Surface.ROTATION_0, 90)
            append(Surface.ROTATION_90, 0)
            append(Surface.ROTATION_180, 270)
            append(Surface.ROTATION_270, 180)
//            append(Surface.ROTATION_0, 270)
//            append(Surface.ROTATION_90, 180)
//            append(Surface.ROTATION_180, 90)
//            append(Surface.ROTATION_270, 0)
        }
    }

    private var bgThread: HandlerThread? = null
    private var bgHandler: Handler? = null

    private var captureSession: CameraCaptureSession? = null
    private lateinit var previewRequestBuilder: CaptureRequest.Builder

    var camRecorder: MediaRecorder? = null
    var screenRecorder: MediaRecorder? = null

    var camDevice: CameraDevice? = null
    var camId: String? = null
    var camChar: CameraCharacteristics? = null

    //A callback object for receiving updates about the state of a camera camDevice.
    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cam: CameraDevice) {
            camDevice = cam
        }
        //Camera no longer available for use
        override fun onDisconnected(cam: CameraDevice) {
            camDevice?.close()
            camDevice = null
        }

        override fun onError(cam: CameraDevice, error: Int) {
            camDevice?.close()
            camDevice = null
            recordActivity.finish()
        }
    }

    @SuppressLint("MissingPermission")
    fun openCam() {
        if(!PermissionsUtil.haveVideoPermission(recordActivity)) {
            Log.i("RecordingSession", "openCam(): don't have required permissions!")
            PermissionsUtil.requestVideoPermission(recordActivity)
            return
        }

        camRecorder = MediaRecorder()
        screenRecorder = MediaRecorder()

        Log.i("RecordingSession", "openCam(): permissions obtained.")

        val manager = recordActivity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            camId = CameraUtil.getFrontCameraId(manager)
            camChar = manager.getCameraCharacteristics(camId ?: "1")

            manager.openCamera(camId!!, stateCallback, null)

        } catch(e: CameraAccessException) {
            Toast.makeText(recordActivity, "Cannot openCam camera", Toast.LENGTH_SHORT).show()
            recordActivity.finish()

        } catch(e: NullPointerException) { //camera2 is used but not supported
            Toast.makeText(recordActivity, "Device not compatible with used API (camera2)", Toast.LENGTH_SHORT).show()
        }
    }

    fun closeCam() {

        try {
            closePreviewSession()
            camDevice?.close()
            camDevice = null

        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        }
    }

    fun setUpCaptureSession() {
        val surfaces = arrayListOf<Surface>(camRecorder!!.surface)

        previewRequestBuilder = camDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            addTarget(camRecorder!!.surface)
        }

        camDevice?.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        captureSession = cameraCaptureSession
                        updatePreview()
                        recordActivity.runOnUiThread {
                            camRecorder?.start()
                            screenRecorder?.start()
                        }
                    }
                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {}
                },
                bgHandler)
    }

    /**
     * Starts a background thread and its [Handler].
     */
    fun startBgThread() {

        bgThread = HandlerThread("CameraBackground").apply {
            start()
        }

        bgHandler = Handler(bgThread?.looper)
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

    fun getLargestCamSize(): Size? {
        val configMap = camChar?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: throw RuntimeException("Cannot retrieve video size")

        return configMap.getOutputSizes(MediaRecorder::class.java).firstOrNull {
            it.width <= 1080
        }
    }

    fun initCamRecorder(path: String, orientation: Int) {

        Log.i("RecordingSession", "initCamRecorder(): path is $path")
        Log.i("RecordingSession", "initCamRecorder(): orientation is $orientation")

        when(camChar?.get(CameraCharacteristics.SENSOR_ORIENTATION)) {
            //Normal orientation
            90 -> {
                camRecorder?.setOrientationHint(ScreenOrientations.NORMAL.get(orientation))
            }
            //Reverse orientation (ends up using the same values for either orientation)
            270 -> {
                camRecorder?.setOrientationHint(ScreenOrientations.INVERSE.get(orientation))
            }
        }

        camRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)

            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(path)
            setVideoEncodingBitRate(8000000) //8Mbps
            setVideoFrameRate(30)
            getLargestCamSize()?.apply {
                setVideoSize(this.width, this.height)
            }

            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

            prepare()
        }
    }

    fun initScreenRecorder(path: String, screenDimen: Size) {

        Log.i("RecordingSession", "initScreenRecorder(): path is $path")

        screenRecorder?.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)

            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(path)
            setVideoEncodingBitRate(2000000) //2Mbps
            setVideoFrameRate(30)
//            getLargestCamSize()?.apply {
//                setVideoSize(this.width, this.height)
//            }
            setVideoSize(screenDimen.width, screenDimen.height)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)

            prepare()
        }
    }

    private fun setUpCaptureRequestBuilder(builder: CaptureRequest.Builder?) {
        builder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
    }

    private fun updatePreview() {
        if (camDevice  == null) return

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

    fun getVideoPaths(ctx: Context): Array<String> {

        val date = Date().stdPattern()
        val directory = ctx.getExternalFilesDir(null)
        directory?.mkdirs()

        val dirPath = directory?.absolutePath
        val cam = CameraUtil.VID_PREFIX_CAM + date + ".mp4"
        val screen = CameraUtil.VID_PREFIX_SCREEN + date + ".mp4"

        return arrayOf("$dirPath/$cam", "$dirPath/$screen")
//        return "${directory?.absolutePath}/$cam"
    }
}