package com.creations.rimov.esbeta.fragments

import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.VideoView
import androidx.fragment.app.Fragment
import com.creations.rimov.esbeta.FrontCamera
import com.creations.rimov.esbeta.R
import com.creations.rimov.esbeta.extensions.gone
import com.creations.rimov.esbeta.extensions.invisible
import com.creations.rimov.esbeta.extensions.visible
import com.creations.rimov.esbeta.util.CameraUtil
import kotlinx.android.synthetic.main.testing_video.view.*

class VideoFragment : Fragment(), View.OnClickListener {

    object PlayStatus {
        const val SET = 700
        const val PLAYING = 701
        const val TOUCHED = 702
    }

    private lateinit var camera: FrontCamera

    private lateinit var vidView: VideoView
    private lateinit var btnStart: ImageButton
    private lateinit var btnStop: ImageButton

    private lateinit var vidUri: Uri
    //State of the video
    private var vidStatus = 0

    private val recorder by lazy {MediaRecorder()}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        camera = FrontCamera(activity ?: return null)
        camera.open()

        vidUri = Uri.parse("android.resource://com.creations.rimov.esbeta/" + R.raw.sample_video)

        inflater.inflate(R.layout.testing_video, container, false)?.let { view ->
            vidView = view.videoView
            btnStart = view.videoBtnStart
            btnStop = view.videoBtnStop

            prepareVideo(vidUri)

            btnStart.setOnClickListener(this)
            btnStop.setOnClickListener(this)
            vidView.setOnClickListener(this)

            return view
        }

        return null
    }

    private fun prepareVideo(uri: Uri): Boolean {

        if(!camera.ready) return false

        vidView.setVideoURI(uri)
        initRecorder()

        setPlayStatus(PlayStatus.SET)

        return true
    }

    private fun startVideo() {

        if(camera.device == null || vidView.isPlaying) return

        //Not certain if needed considering no preview has been set up
//        camera.device.createCaptureSession(
//            listOf<Surface>(),
//            object : CameraCaptureSession.StateCallback() {
//                override fun onConfigureFailed(p0: CameraCaptureSession) {
//
//                }
//
//                override fun onConfigured(p0: CameraCaptureSession) {
//
//                }
//            }, )

        setPlayStatus(PlayStatus.PLAYING)

        recorder.start()
        vidView.start()
    }

    private fun stopVideo() {

        vidView.stopPlayback()
        recorder.apply {
            stop()
            reset()
        }

        //Reset video
        prepareVideo(vidUri)
        setPlayStatus(PlayStatus.SET)
    }

    private fun initRecorder() {

        val vid = CameraUtil.getNewVideoUri(context ?: return)

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.CAMERA)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setOutputFile(vid?.path)
            Log.i("VideoFrag", "initRecorder(): saving in path ${vid?.path}")

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