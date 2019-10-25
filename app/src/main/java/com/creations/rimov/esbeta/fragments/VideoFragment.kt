package com.creations.rimov.esbeta.fragments

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
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import android.widget.VideoView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.creations.rimov.esbeta.RecordingSession
import com.creations.rimov.esbeta.R
import com.creations.rimov.esbeta.extensions.*
import com.creations.rimov.esbeta.util.CameraUtil
import com.creations.rimov.esbeta.util.PermissionsUtil
import kotlinx.android.synthetic.main.testing_video.view.*
import java.util.*

class VideoFragment : Fragment(), View.OnClickListener {

    object PlayStatus {
        const val SET = 700
        const val PLAYING = 701
        const val TOUCHED = 702
    }

    object Constants {
        const val VIRTUAL_DISPLAY_NAME = "ScreenRec"
    }

    private val TAG = this::class.java.simpleName
    private val displayMetrics by lazy { DisplayMetrics() }
    private var screenOrientation: Int = 0

    private lateinit var recordingSession: RecordingSession

    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var projection: MediaProjection
    private lateinit var virtualDisplay: VirtualDisplay

    private lateinit var video: VideoView
    private lateinit var btnStart: ImageButton
    private lateinit var btnStop: ImageButton

    private lateinit var vidUri: Uri
    //State of the video
    private var vidStatus = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        activity?.let {
            it.windowManager.defaultDisplay.let { display ->
                display.getMetrics(displayMetrics)
                screenOrientation = display.rotation
            }

            it.windowManager.defaultDisplay.getMetrics(displayMetrics)

            getScreenCaptureManager()
            recordingSession = RecordingSession(it).apply {
                openCam()
            }
        }

//        camRecorder = MediaRecorder()

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

        recordingSession.apply {
            startBgThread()
            openCam()
        }
//        camRecorder = MediaRecorder()
    }

    override fun onPause() {

        recordingSession.apply {
            closeCam()
            stopBgThread()
        }

        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when(requestCode) {
            PermissionsUtil.MEDIA_PROJECTION_REQUEST -> {
                data?.let {
                    projection = projectionManager.getMediaProjection(resultCode, it)
                }
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

    private fun prepareVideo(uri: Uri) {

        video.setVideoURI(uri)

        setPlayStatus(PlayStatus.SET)
    }

    private fun startVideo() {
        if(recordingSession.camDevice == null || video.isPlaying) {
            Toast.makeText(context, "Cannot start video! Device null? ${recordingSession.camDevice == null}", Toast.LENGTH_SHORT).show()
            return
        }

        //[0] is the camera path, [1] is the screen path
        val paths: Array<String> = recordingSession.getVideoPaths(context ?: return)

        recordingSession.initCamRecorder(paths[0], screenOrientation)
        recordingSession.initScreenRecorder(paths[1])
        // Set up Surface for recordingSession preview and MediaRecorder
        recordingSession.setUpCaptureSession()

        initVirtualDisplay()

        setPlayStatus(PlayStatus.PLAYING)

        video.start()
    }

    private fun stopVideo() {

        video.stopPlayback()

        projection.stop()
        virtualDisplay.release()

        recordingSession.closeCam()
        recordingSession.camRecorder?.apply {
            stop()
            reset()
        }

        recordingSession.screenRecorder?.apply {
            stop()
            reset()
        }

        //Reset video
        video.suspend()
        prepareVideo(vidUri)
    }

    private fun initVirtualDisplay() {

        if(!::projection.isInitialized) return

        val size = recordingSession.camSize ?: return

        virtualDisplay = projection.createVirtualDisplay(
            Constants.VIRTUAL_DISPLAY_NAME,
            size.width,
            size.height,
            displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            recordingSession.screenRecorder?.surface, null, null)
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
//                btnStop.gone() Some issue with registering VideoView clicks on tablet, forced to keep btnStop visible always
                btnStop.visible()
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
}