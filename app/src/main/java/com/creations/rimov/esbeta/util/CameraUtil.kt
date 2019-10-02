package com.creations.rimov.esbeta.util

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Environment
import android.util.Log
import com.creations.rimov.esbeta.extensions.stdPattern
import java.io.File
import java.util.*

object CameraUtil {

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
    fun getVideoPath(context: Context): String {
        val directory = context.getExternalFilesDir(null)
        directory?.mkdirs()

        val name = VID_FILENAME_PREFIX + Date().stdPattern() + ".mp4"

        return "${directory?.absolutePath}/$name"
    }

    @JvmStatic
    private fun createVideoFile(storageDir: File): File {

        val time = Date().stdPattern() //Part of the file name
        val file = File(storageDir, "$VID_FILENAME_PREFIX$time.mp4")

        Log.i("CameraUtil", "createVideoFile(): created file: ${file.absolutePath}")

        return file
    }

    @JvmStatic
    private fun isExternalStorageAvailable(): Boolean
            = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
}