package com.creations.rimov.esbeta.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import android.widget.VideoView
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import com.creations.rimov.esbeta.FrontCamera
import com.creations.rimov.esbeta.R
import com.creations.rimov.esbeta.extensions.gone
import com.creations.rimov.esbeta.extensions.invisible
import com.creations.rimov.esbeta.extensions.visible
import com.creations.rimov.esbeta.util.CameraUtil
import com.creations.rimov.esbeta.util.PermissionsUtil
import kotlinx.android.synthetic.main.testing_video.view.*

class VideoFragment : Fragment(), View.OnClickListener {

    object PlayStatus {
        const val SET = 700
        const val PLAYING = 701
        const val TOUCHED = 702
    }

    object Constants {
        const val VIRTUAL_DISPLAY_NAME = "ScreenRec"
    }

    private val displayMetrics by lazy { DisplayMetrics() }

    private lateinit var camera: FrontCamera

    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var projection: MediaProjection
    private lateinit var virtualDisplay: VirtualDisplay

    private lateinit var video: VideoView
    private lateinit var btnStart: ImageButton
    private lateinit var btnStop: ImageButton

    private lateinit var vidUri: Uri
    //State of the video
    private var vidStatus = 0

    private var recorder: MediaRecorder? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        activity?.let {
            it.windowManager.defaultDisplay.getMetrics(displayMetrics)

            getScreenCaptureManager()
            camera = FrontCamera(it).apply {
                open()
            }
        }

        recorder = MediaRecorder()

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

    override fun onResume() {
        super.onResume()

        camera.apply {
            startBgThread()
            open()
        }

        recorder = MediaRecorder()
    }

    override fun onPause() {

        camera.apply {
            close()
            stopBgThread()
        }

        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        Log.i("VideoFrag", "onActivityResult(): requestCode is media projection? ${requestCode == PermissionsUtil.MEDIA_PROJECTION_REQUEST}")

        when(requestCode) {
            PermissionsUtil.MEDIA_PROJECTION_REQUEST -> {
                data?.let {
                    projection = projectionManager.getMediaProjection(resultCode, it)
                }
            }
        }
    }

    private fun prepareVideo(uri: Uri) {

        video.setVideoURI(uri)

        setPlayStatus(PlayStatus.SET)
    }

    private fun startVideo() {
        if(camera.device == null || video.isPlaying) {
            Toast.makeText(context, "Cannot start video! Device null? ${camera.device == null}", Toast.LENGTH_SHORT).show()
            return
        }

        initVirtualDisplay()
        initRecorder()
        // Set up Surface for camera preview and MediaRecorder
        camera.setUpCaptureSession(arrayListOf(recorder!!.surface), recorder)

        setPlayStatus(PlayStatus.PLAYING)

        video.start()
    }

    private fun stopVideo() {

        video.stopPlayback()
        camera.close()
        recorder?.apply {
            stop()
            reset()
//            release()
        }

        projection.stop()
        virtualDisplay.release()

//        recorder = null

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
            setVideoFrameRate(30)
            camera.getLargestSize()?.apply {
                setVideoSize(this.width, this.height)
            }

            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

            prepare()
        }
    }

    private fun initVirtualDisplay() {

        if(!::projection.isInitialized) return

        virtualDisplay = projection.createVirtualDisplay(
            Constants.VIRTUAL_DISPLAY_NAME,
            video.measuredWidth,
            video.measuredHeight,
            displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            recorder?.surface, null, null)
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

    private fun getScreenCaptureManager() {
        projectionManager = activity?.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        startActivityForResult(projectionManager.createScreenCaptureIntent(), PermissionsUtil.MEDIA_PROJECTION_REQUEST)
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