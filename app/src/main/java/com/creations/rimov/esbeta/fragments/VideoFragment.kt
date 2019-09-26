package com.creations.rimov.esbeta.fragments

import android.media.CamcorderProfile
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
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class VideoFragment : Fragment() {

    private lateinit var vidView: VideoView
    private lateinit var vidUri: Uri

    private val btnStart by lazy {videoBtnStart}
    private val btnStop by lazy {videoBtnStop}

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

        if(vidView.isPlaying) return

        vidUri = Uri.parse("android.resource://com.creations.rimov.esbeta/" + R.raw.sample_video)

        vidView.setVideoURI(vidUri)
        vidView.start()
    }

    private fun stopVideo() {
        if(vidView.isPlaying) vidView.stopPlayback()
    }

    private fun initRecorder() {

        val vid = File(context?.filesDir,
            SimpleDateFormat("MM/dd_HHmmss", Locale.US)".mp4")

        recorder.apply {
            setVideoSource(MediaRecorder.VideoSource.CAMERA)
//            setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH))
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
//            setVideoSize()
            setOutputFile(vid.absolutePath)
            Log.i("VideoFrag", "initRecorder(): saving in path ${vid.absolutePath}")
        }
    }

    private fun prepareRecorder() {

        try {
            recorder.prepare()

        } catch(e: IOException) {
            e.printStackTrace()
        }
    }

//    private fun getFrontCameraId(): Int {
//
//        //android.hardware.camera2. ---- use the MediaRecorder but first you need to let it know to use the front camera
//    }
}