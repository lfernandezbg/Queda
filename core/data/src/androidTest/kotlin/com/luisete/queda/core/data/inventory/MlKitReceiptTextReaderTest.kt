package com.luisete.queda.core.data.inventory

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.luisete.queda.core.domain.inventory.ReceiptRecognition
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MlKitReceiptTextReaderTest {
    @Test
    fun bundledRecognizerReadsAnActualImageAndRequiresReview() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val bitmap = Bitmap.createBitmap(1500, 800, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    textSize = 70f
                }
            canvas.drawText("LECHE ENTERA 1,20", 80f, 180f, paint)
            canvas.drawText("PAN INTEGRAL 0,90", 80f, 340f, paint)
            canvas.drawText("TOTAL 2,10", 80f, 500f, paint)
            val file = File(context.cacheDir, "ocr-regression.png")
            try {
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                val result = MlKitReceiptTextReader(context).recognize(Uri.fromFile(file).toString())
                assertTrue(result is ReceiptRecognition.Recognized)
                val lines = (result as ReceiptRecognition.Recognized).draft.lines
                assertTrue(lines.any { it.name.contains("LECHE") })
                assertTrue(lines.any { it.name.contains("PAN") })
                assertFalse(lines.any { it.selected || it.reviewed || it.name.startsWith("TOTAL") })
            } finally {
                bitmap.recycle()
                file.delete()
            }
        }
}
