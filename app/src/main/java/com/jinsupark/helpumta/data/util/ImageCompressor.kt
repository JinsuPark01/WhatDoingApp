package com.jinsupark.helpumta.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun compress(
        uri: Uri,
        maxSize: Int = MAX_SIZE,
        quality: Int = QUALITY
    ): ByteArray = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        // 1. 크기만 먼저 읽기 (메모리에 안 올림)
        //    inJustDecodeBounds=true면 decodeStream은 항상 null을 반환하므로
        //    스트림 null 체크와 디코드 결과를 분리해야 함
        val boundsStream = resolver.openInputStream(uri) ?: error("이미지를 열 수 없습니다")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) error("이미지를 읽을 수 없습니다")

        // 2. 샘플링 디코드 (OOM 방어)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxSize)
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: error("이미지를 디코드할 수 없습니다")

        // 3. EXIF 회전 각도 (실패해도 업로드는 진행)
        val rotation = runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }
        }.getOrNull() ?: 0f

        // 4. 스케일 + 회전
        val scaled = scaleDown(decoded, maxSize)
        val rotated = if (rotation != 0f) {
            val matrix = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
                .also { if (it != scaled) scaled.recycle() }
        } else scaled

        // 5. JPEG 인코딩
        val output = ByteArrayOutputStream()
        rotated.compress(Bitmap.CompressFormat.JPEG, quality, output)
        rotated.recycle()

        output.toByteArray()
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxSize: Int): Int {
        var sampleSize = 1
        var longSide = maxOf(width, height)
        while (longSide / 2 >= maxSize) {
            longSide /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun scaleDown(bitmap: Bitmap, maxSize: Int): Bitmap {
        val longSide = maxOf(bitmap.width, bitmap.height)
        if (longSide <= maxSize) return bitmap

        val ratio = maxSize.toFloat() / longSide
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt(),
            (bitmap.height * ratio).toInt(),
            true
        )
        if (scaled != bitmap) bitmap.recycle()
        return scaled
    }

    companion object {
        const val MAX_SIZE = 1920
        const val QUALITY = 85
    }
}