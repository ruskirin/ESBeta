package com.creations.rimov.esbeta.fragments

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.creations.rimov.esbeta.R
import com.creations.rimov.esbeta.RecordingSession
import com.creations.rimov.esbeta.extensions.gone
import com.creations.rimov.esbeta.extensions.infoLog
import com.creations.rimov.esbeta.extensions.shortToast
import com.creations.rimov.esbeta.util.PermissionsUtil
import com.creations.rimov.esbeta.view_models.BookViewModel
import com.creations.rimov.esbeta.view_models.GlobalViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.testing_book.view.*

/**
 * Used the following for guidance:
 *   https://medium.com/@chahat.jain0/rendering-a-pdf-document-in-android-activity-fragment-using-pdfrenderer-442462cb8f9a
 */
class BookFragment : Fragment(), View.OnClickListener {
    @JvmField val BOOKS = mapOf(1 to "esbook_1.pdf", 2 to "esbook_2.pdf")

    private val TAG = this::class.java.simpleName

    private lateinit var globalVm: GlobalViewModel
    private val localVm: BookViewModel by lazy {
        ViewModelProviders.of(this).get(BookViewModel::class.java)
    }

    private val args: BookFragmentArgs by navArgs()

    private val displayMetrics by lazy { DisplayMetrics() }
    private var screenOrientation: Int = 0

    private lateinit var recordingSession: RecordingSession

    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var projection: MediaProjection
    private lateinit var virtualDisplay: VirtualDisplay

    private lateinit var pageImage: ImageView
    private lateinit var btn: ImageView
    private lateinit var fab: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.let {
            globalVm = ViewModelProviders.of(it).get(GlobalViewModel::class.java)

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

        TAG.infoLog("onCreate(): Requested book ${args.book}, ${BOOKS[args.book]}")
        //TODO: display error image
        localVm.initRenderer(BOOKS[args.book] ?: "esbook_1.pdf")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.testing_book, container, false)

        pageImage = view.bookPage
        btn = view.bookBtn
        fab = view.bookFab

        globalVm.getPageNum().observe(this, Observer { num ->
            localVm.setPageNum(num)

            if(num == localVm.getTotalPageNum()-1) fab.show()
            else fab.hide()

            Glide.with(this)
                .load(localVm.getRenderedPage(displayMetrics.widthPixels))
                .centerCrop()
                .into(pageImage)
        })

        btn.setOnClickListener(this)
        fab.setOnClickListener(this)

        return view
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

    override fun onClick(view: View?) {
        when(view?.id) {
            R.id.bookBtn -> {
                toggleBtn()
            }
            R.id.bookFab -> {
                stop()
            }
        }
    }

    private fun start() {

        globalVm.setTotalPageNum(localVm.getTotalPageNum())
        globalVm.setPageNum(0)

        if(recordingSession.camDevice == null) {
            context?.shortToast("Cannot start video! Device null? ${recordingSession.camDevice == null}")
            return
        }

        //[0] is the camera path, [1] is the screen path
        val paths: Array<String> = recordingSession.getVideoPaths(context ?: return)

        TAG.infoLog("Saving in paths: ${paths[0]}, ${paths[1]}")

        recordingSession.initCamRecorder(paths[0], screenOrientation)
        recordingSession.initScreenRecorder(paths[1])
        // Set up Surface for recordingSession preview and MediaRecorder
        recordingSession.setUpCaptureSession()

        initVirtualDisplay()
    }

    private fun stop() {

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
    }

    private fun initVirtualDisplay() {

        if(!::projection.isInitialized) return

        val size = recordingSession.camSize ?: return

        virtualDisplay = projection.createVirtualDisplay(
            VideoFragment.Constants.VIRTUAL_DISPLAY_NAME,
            size.width,
            size.height,
            displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            recordingSession.screenRecorder?.surface, null, null)
    }

    private fun toggleBtn() {
        val startImg = resources.getDrawable(R.drawable.ic_play, null)
        val endImg = resources.getDrawable(R.drawable.ic_stop, null)
        val startTxt = resources.getString(R.string.testing_book_btn_begin)
        val endTxt = resources.getString(R.string.testing_book_btn_end)

        if(btn.tag == startTxt) {
            TAG.infoLog("Toggling start button")

            btn.setImageDrawable(endImg)
            btn.tag = endTxt
            btn.gone()

            start()

        } else {
            TAG.infoLog("Toggling stop button")

            btn.setImageDrawable(startImg)
            btn.tag = startTxt
            btn.gone()

            stop()
        }
    }

    private fun getScreenCaptureManager() {
        projectionManager = activity?.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        startActivityForResult(projectionManager.createScreenCaptureIntent(), PermissionsUtil.MEDIA_PROJECTION_REQUEST)
    }
}