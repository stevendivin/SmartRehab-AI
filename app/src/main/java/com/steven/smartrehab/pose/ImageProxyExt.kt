package com.steven.smartrehab.pose

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy

fun ImageProxy.toBitmap(): Bitmap {
    val planeProxy = planes[0]
    val buffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}