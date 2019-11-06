package com.creations.rimov.esbeta.view_models

import android.app.Application
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.AndroidViewModel
import com.creations.rimov.esbeta.R
import com.creations.rimov.esbeta.extensions.infoLog
import com.creations.rimov.esbeta.extensions.shortToast
import com.creations.rimov.esbeta.fragments.BookFragment
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class BookViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = this::class.java.simpleName

    private lateinit var renderer: PdfRenderer

    private var bookName: String = ""

    private var pageNum: Int = 0
    private var page: PdfRenderer.Page? = null

    fun getPageNum() = pageNum

    fun setPageNum(num: Int) {

        if(num >= renderer.pageCount) {
            (getApplication() as Application).shortToast("No such page!")
            return
        }

        pageNum = num
    }

    fun getRenderedPage(width: Int? = null): Bitmap? {
        if(page != null) page!!.close() //Only 1 pdfPage can be rendered at a time

        TAG.infoLog("Rendering page $pageNum")

        var bitmap: Bitmap

        page = renderer.openPage(pageNum)
            .apply {
                val ratio = this.width.toFloat()/this.height

                if(width == null)
                    bitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
                else
                    bitmap = Bitmap.createBitmap(width, (width/ratio).roundToInt(), Bitmap.Config.ARGB_8888)

                render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        }

        return bitmap
    }

    fun initRenderer(bookName: String) {

        if(::renderer.isInitialized && (bookName == this.bookName)) return

        pageNum = 0 //Reset pageNum

        val ctx = getApplication<Application>()

        val resource =
            if(bookName == "esbook_1.pdf")  R.raw.esbook_1
            else if(bookName == "esbook_2.pdf") R.raw.esbook_2
            else R.raw.esbook_2

        //An object from raw cannot be directly made into a File, must be cached first
        val file = File(ctx.cacheDir, bookName)

        val input = ctx.resources.openRawResource(resource)
        val output = FileOutputStream(file)
        val buffer = ByteArray(1024)

        input.apply {
            var size = read(buffer)

            while(size != -1) {
                output.write(buffer, 0, size)

                size = read(buffer)
            }

            close()
            output.close()
        }

        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)?.let {
            renderer = PdfRenderer(it)
        }
    }

    fun getTotalPageNum() = renderer.pageCount
}