package com.creations.rimov.esbeta.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import android.widget.VideoView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.creations.rimov.esbeta.R
import com.creations.rimov.esbeta.extensions.gone
import com.creations.rimov.esbeta.extensions.invisible
import com.creations.rimov.esbeta.extensions.visible
import com.creations.rimov.esbeta.util.CameraUtil
import kotlinx.android.synthetic.main.testing_video.view.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class VideoFragment : Fragment(), View.OnClickListener {

    object PlayStatus {
        const val SET = 700
        const val PLAYING = 701
        const val TOUCHED = 702
    }

    private lateinit var vidView: VideoView
    private lateinit var btnStart: ImageButton
    private lateinit var btnStop: ImageButton

    private lateinit var vidUri: Uri
    //State of the video
    private var vidStatus = 0

    private val recorder by lazy {MediaRecorder()}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        if(!getPermissions()) return null

        vidUri = Uri.parse("android.resource://com.creations.rimov.esbeta/" + R.raw.sample_video)

        inflater.inflate(R.layout.testing_video, container, false)?.let { view ->
            vidView = view.videoView
            btnStart = view.videoBtnStart
            btnStop = view.videoBtnStop

            prepareVideo(vidUri)

            btnStart.setOnClickListener(this)
            btnStop.setOnClickListener(this)
            vidView.setOnClickListener(this)

//            vidView.setOnCompletionListener {
//                stopVideo()
//            }

            return view
        }

        return null
    }

    private fun prepareVideo(uri: Uri): Boolean {

        vidView.setVideoURI(uri)
        initRecorder()

        if(!prepareRecorder()) {
            Toast.makeText(context,
                "Something went with the camera! Aborting.", Toast.LENGTH_LONG).show()

            return false
        }

        setPlayStatus(PlayStatus.SET)

        return true
    }

    private fun startVideo() {

        if(vidView.isPlaying) return

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

        val vid = CameraUtil.getVideoFile()

        recorder.apply {
//            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.CAMERA)
//            setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH))
            setOutputFormat(MediaRecorder.OutputFormat.WEBM)
//            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setVideoEncoder(MediaRecorder.VideoEncoder.VP8)
            setVideoSize(480, 360)
            setOutputFile(vid?.absolutePath)
            Log.i("VideoFrag", "initRecorder(): saving in path ${vid?.absolutePath}")
        }
    }

    //If prepare() fails, return false
    private fun prepareRecorder(): Boolean {

        try {
            recorder.prepare()

        } catch(e: IOException) {
            e.printStackTrace()
            return false
        }

        return true
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

    //Since SDK 23(24?), permission must be requested at runtime if it has not already been granted
    private fun getPermissions(): Boolean {

        context?.let { myContext ->
            activity?.let { myActivity ->
                if((ContextCompat.checkSelfPermission(
                        myContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                    && (ContextCompat.checkSelfPermission(
                        myContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {

                    return true //Permission has already been granted
                }

                //Explain why you need the permission
                if(ActivityCompat.shouldShowRequestPermissionRationale(
                        myActivity, Manifest.permission.CAMERA)) {
                    //TODO FUTURE: display rationale for this request
                    Toast.makeText(myContext, "Camera is required for app functionality", Toast.LENGTH_LONG).show()
                }

                //Permission has not yet been granted, check onRequestPermissionResult()
                ActivityCompat.requestPermissions(
                    //Request code is personal but must be constant for any future checks
                    myActivity,
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1000)
            }
        }

        return false
    }

//    private fun getFrontCameraId(): Int {
//
//        //android.hardware.camera2. ---- use the MediaRecorder but first you need to let it know to use the front camera
//    }
}