package com.creations.rimov.esbeta.util

import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.creations.rimov.esbeta.extensions.stdPattern
import java.io.File
import java.io.IOException
import java.util.*

object CameraUtil {

//    private const val IMAGE_PROVIDER_AUTHORITY = "com.creations.rimov.esbeta.fileprovider"
    private const val VID_FILENAME_PREFIX = "EarlySee_vid_react_"

    @JvmStatic
    fun getFrontCameraId(cameraManager: CameraManager): String? {
        val camerasList =
            try {
                cameraManager.cameraIdList
            } catch(e: CameraAccessException) {
                Log.e("CameraUtil", "getFrontCameraId(): CameraAccessException thrown!")
                return null
            }

        camerasList.forEach { camera ->
            val camChar = cameraManager.getCameraCharacteristics(camera).get(CameraCharacteristics.LENS_FACING)

            if(camChar == CameraCharacteristics.LENS_FACING_FRONT) return camera
        }

        return null
    }

    @JvmStatic
    fun getVideoFile(storageDir: File? = null): File? {

        //TODO FUTURE: handle this
        if(!isExternalStorageAvailable()) {
            Log.e("Image Creation", "CameraUtil#getImageFile(): external storage not available!")
            return null
        }

        return try {
            //Starting SDK 29, getExternalStoragePublicDirectory is deprecated and other methods need to be used
            val directory = storageDir ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)

            createImageFile(directory)

        } catch(e: IOException) {
            e.printStackTrace()
            null
        }
    }

//    @JvmStatic
//    fun getImageUri(context: Context, imageFile: File): Uri =
//        FileProvider.getUriForFile(context, IMAGE_PROVIDER_AUTHORITY, imageFile)

    @JvmStatic
    fun getNewVideoUri(context: Context): Uri? {

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, VID_FILENAME_PREFIX + Date().stdPattern())
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/ESBeta")
        }

        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    @JvmStatic
    fun deleteImageFile(imagePath: String) = File(imagePath).delete()

    @JvmStatic
    private fun createImageFile(storageDir: File): File {

        val time = Date().stdPattern() //Part of the file name
        val file = File(storageDir, "$VID_FILENAME_PREFIX$time.webm")

        return file
    }

    @JvmStatic
    private fun isExternalStorageAvailable(): Boolean
            = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
}