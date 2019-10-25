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
import com.creations.rimov.esbeta.extensions.infoLog
import com.creations.rimov.esbeta.extensions.stdPattern
import com.creations.rimov.esbeta.util.CameraUtil
import com.creations.rimov.esbeta.util.PermissionsUtil
import java.lang.NullPointerException
import java.lang.RuntimeException
import java.util.*
import kotlin.Comparator

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
    var camSize: Size? = null

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

            camSize = getWorkingCamSize()

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

    fun startBgThread() {

        bgThread = HandlerThread("CameraBackground").apply {
            start()
        }

        bgHandler = Handler(bgThread?.looper)
    }

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

    fun getWorkingCamSize(): Size? {
        val configMap = camChar?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: throw RuntimeException("Cannot retrieve video size")

        val selected = configMap.getOutputSizes(MediaRecorder::class.java).maxWith(
            object: Comparator<Size> {
                override fun compare(s1: Size, s2: Size) = when {
                    (s1.height == 720) && (s1.width > s2.width) -> 1
                    (s1.height == 720) && (s1.width == s2.width) -> 0
                    else -> -1
                }
            })

        recordActivity::class.java.simpleName.infoLog("Selected size: ${selected?.width} by ${selected?.height}")
        return selected
    }

    fun initCamRecorder(path: String, orientation: Int) {

        Log.i("RecordingSession", "initCamRecorder(): path is $path")

        when(camChar?.get(CameraCharacteristics.SENSOR_ORIENTATION)) {
            //Normal orientation
            90 -> camRecorder?.setOrientationHint(ScreenOrientations.NORMAL.get(orientation))
            //TODO: output video captured through a certain horizontal rotation flips the video, explore Android official docs for possible solution (possible one saw before)
            270 -> camRecorder?.setOrientationHint(ScreenOrientations.INVERSE.get(orientation))
        }

        camRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

            setVideoFrameRate(20)
            setVideoEncodingBitRate(16000000) //16Mbps
            camSize?.let { setVideoSize(it.width, it.height) }

            setOutputFile(path)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

            prepare()
        }
    }

    fun initScreenRecorder(path: String) {

        Log.i("RecordingSession", "initScreenRecorder(): path is $path")

        screenRecorder?.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

            setVideoFrameRate(20)
            setVideoEncodingBitRate(2000000) //2Mbps
            camSize?.let { setVideoSize(it.width, it.height) }

            setOutputFile(path)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)

            prepare()
        }
    }

    private fun setUpCaptureRequestBuilder(builder: CaptureRequest.Builder?) {

        builder?.apply {
            set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            set(CaptureRequest.EDGE_MODE, CameraMetadata.EDGE_MODE_HIGH_QUALITY)
//            set(CaptureRequest.TONEMAP_MODE, CameraMetadata.TONEMAP_MODE_HIGH_QUALITY)
        }
    }

    private fun updatePreview() {
        if (camDevice  == null) return

        try {
            setUpCaptureRequestBuilder(previewRequestBuilder)
            HandlerThread("CameraPreview").start()
            captureSession?.setRepeatingRequest(
                previewRequestBuilder.build(), null, bgHandler)
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
    }
}