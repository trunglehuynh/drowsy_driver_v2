package com.galaxylab.drowsydriver.AI

import android.util.Size
import androidx.camera.core.CameraSelector

//1.77777777778

class CameraConfiguration {
    companion object {
        val minBoundingBox: Int = 100
        val with: Int = 720
        val height: Int = 960
        val facingLen: Int = CameraSelector.LENS_FACING_FRONT
        val portraitSize: Size = Size(with, height)
        val landscapeSize: Size = Size(height, with)
    }
}