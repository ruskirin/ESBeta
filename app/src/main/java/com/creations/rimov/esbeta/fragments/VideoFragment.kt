package com.creations.rimov.esbeta.fragments

import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import androidx.fragment.app.Fragment
import com.creations.rimov.esbeta.R
import kotlinx.android.synthetic.main.testing_video.*
import kotlinx.android.synthetic.main.testing_video.view.*

class VideoFragment : Fragment() {

    private lateinit var vidView: VideoView
    private lateinit var vidUri: Uri

    private val recorder by lazy {MediaRecorder()}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        inflater.inflate(R.layout.testing_video, container, false)?.let { view ->
            vidView = view.videoView
            return view
        }

        return null
    }

    private fun startVideo() {

        vidUri = Uri.parse("android.resource://com.creations.rimov.esbeta/" + R.raw.sample_video)

        vidView.setVideoURI(vidUri)
        vidView.start()
    }

    private fun resumeVideo() {
        if(!vidView.isPlaying) vidView.resume()
    }

    private fun pauseVideo() {
        if(vidView.canPause()) vidView.pause()
    }

    private fun stopVideo() {
        if(vidView.isPlaying) vidView.stopPlayback()
    }

    private fun initRecorder() {

        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA)
    }

    private fun getFrontCameraId(): Int {

        //android.hardware.camera2. ---- use the MediaRecorder but first you need to let it know to use the front camera
    }
}