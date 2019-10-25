package com.creations.rimov.esbeta.view_models

import android.app.Application
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.creations.rimov.esbeta.R
import java.io.File
import java.io.FileOutputStream

class BookViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var renderer: PdfRenderer

    private var bookName: String = ""
    private val pageNum: MutableLiveData<Int> = MutableLiveData(0)
    private var page: PdfRenderer.Page? = null

    fun getPageNum() = pageNum

    fun setPageNum(num: Int) {

        if(num >= renderer.pageCount) {
            Toast.makeText(getApplication<Application>(), "No such page!", Toast.LENGTH_SHORT).show()
            return
        }

        pageNum.postValue(num)
    }

    fun getRenderedPage(): Bitmap? {

        if(page != null) page!!.close() //Only 1 pdfPage can be rendered at a time

        var bitmap: Bitmap

        page = renderer.openPage(pageNum.value ?: return null)
            .apply {
                bitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)

                render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        }

        return bitmap
    }

    fun initRenderer(bookName: String) {
        if(::renderer.isInitialized && (bookName == this.bookName)) return

        pageNum.postValue(0) //Reset pageNum

        val ctx = getApplication<Application>()

        //An object from raw cannot be directly made into a File, must be cached first
        val file = File(ctx.cacheDir, bookName)

        val input = ctx.resources.openRawResource(R.raw.esbook_1)
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
}