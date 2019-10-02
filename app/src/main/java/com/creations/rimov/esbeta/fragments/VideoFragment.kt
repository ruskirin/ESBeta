package com.creations.rimov.esbeta.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import android.widget.VideoView
import androidx.fragment.app.Fragment
import com.creations.rimov.esbeta.FrontCamera
import com.creations.rimov.esbeta.R
import com.creations.rimov.esbeta.extensions.gone
import com.creations.rimov.esbeta.extensions.invisible
import com.creations.rimov.esbeta.extensions.visible
import com.creations.rimov.esbeta.util.CameraUtil
import com.creations.rimov.esbeta.util.PermissionsUtil
import kotlinx.android.synthetic.main.testing_video.view.*
import java.lang.NullPointerException
import java.lang.RuntimeException

class VideoFragment : Fragment(), View.OnClickListener {

    object PlayStatus {
        const val SET = 700
        const val PLAYING = 701
        const val TOUCHED = 702
    }

    private lateinit var cameraActivity: Activity

//    private lateinit var camera: FrontCamera

    private lateinit var video: VideoView
    private lateinit var btnStart: ImageButton
    private lateinit var btnStop: ImageButton

    private lateinit var vidUri: Uri
    //State of the video
    private var vidStatus = 0

    private var recorder: MediaRecorder? = null

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        cameraActivity = activity as Activity

//        camera = FrontCamera(activity ?: return null)
//        if(camera.open())
//            Log.i("VideoFrag", "onCreateView(): camera opened.")
        open()

        vidUri = Uri.parse("android.resource://com.creations.rimov.esbeta/" + R.raw.sample_video)

        inflater.inflate(R.layout.testing_video, container, false)?.let { view ->
            video = view.videoView
            btnStart = view.videoBtnStart
            btnStop = view.videoBtnStop

            prepareVideo(vidUri)

            btnStart.setOnClickListener(this)
            btnStop.setOnClickListener(this)
            video.setOnClickListener(this)

            return view
        }

        return null
    }

    @SuppressLint("MissingPermission")
    fun open() {

        if(!PermissionsUtil.haveVideoPermission(cameraActivity)) {
            Log.i("FrontCamera", "open(): don't have required permissions!")
            PermissionsUtil.requestVideoPermission(cameraActivity)
        }

        Log.i("FrontCamera", "open(): permissions obtained.")

        val manager = cameraActivity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            id = CameraUtil.getFrontCameraId(manager)
            char = manager.getCameraCharacteristics(id ?: return)

            recorder = MediaRecorder()
            manager.openCamera(id!!, stateCallback, null)

        } catch(e: CameraAccessException) {
            Toast.makeText(activity, "Cannot open camera", Toast.LENGTH_SHORT).show()
            cameraActivity.finish()

        } catch(e: NullPointerException) { //camera2 is used but not supported
            Toast.makeText(activity, "Device not compatible with used API (camera2)", Toast.LENGTH_SHORT).show()
        }
    }

    fun close() {

        device?.close()
        device = null
        recorder?.release()
        recorder = null
    }

    fun getVideoSize(): Size? {
        val configMap = char?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: throw RuntimeException("Cannot retrieve video size")

        return configMap.getOutputSizes(MediaRecorder::class.java).firstOrNull { it.width <= 1080 }
    }

    private fun prepareVideo(uri: Uri) {

        video.setVideoURI(uri)

        setPlayStatus(PlayStatus.SET)
    }

    private fun startVideo() {
        if(device == null || video.isPlaying) {
            Toast.makeText(context, "Cannot start video! Device null? ${device == null}", Toast.LENGTH_SHORT).show()
            return
        }

        initRecorder()

        setPlayStatus(PlayStatus.PLAYING)

        recorder?.start()
        video.start()
    }

    private fun stopVideo() {

        video.stopPlayback()
        close()
        recorder?.apply {
            stop()
            reset()
        }

        //Reset video
        prepareVideo(vidUri)
        setPlayStatus(PlayStatus.SET)
    }

    private fun initRecorder() {
        val ctx = context ?: return

        val path = CameraUtil.getVideoPath(ctx)
        Log.i("VideoFrag", "initRecorder(): path is $path")

        recorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)

            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(path)

            setVideoFrameRate(15)
            getVideoSize()?.apply {
                setVideoSize(this.width, this.height)
            }

            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)

            prepare()
        }
    }

    private fun setPlayStatus(status: Int) {

        vidStatus = status

        when(status) {
            PlayStatus.SET -> {
                btnStart.visible()
                btnStop.gone()
            }
            PlayStatus.PLAYING -> {
                btnStart.gone()
                btnStop.invisible()
            }
            PlayStatus.TOUCHED -> {
                btnStop.visible()
            }
        }
    }

    override fun onClick(v: View?) {

        v?.let {
            when(it.id) {
                R.id.videoBtnStart -> {
                    startVideo()
                }
                R.id.videoBtnStop -> {
                    stopVideo()
                }
                R.id.videoView -> {
                    if(vidStatus == PlayStatus.TOUCHED)
                        setPlayStatus(PlayStatus.PLAYING)
                    else
                        setPlayStatus(PlayStatus.TOUCHED)
                }
            }
        }
    }
}