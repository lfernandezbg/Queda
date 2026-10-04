package com.luisete.queda.core.data.inventory

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.luisete.queda.core.data.household.awaitTask
import com.luisete.queda.core.domain.inventory.ReceiptParser
import com.luisete.queda.core.domain.inventory.ReceiptRecognition
import com.luisete.queda.core.domain.inventory.ReceiptTextReader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.Executor
import javax.inject.Inject

class MlKitReceiptTextReader
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ReceiptTextReader {
        override suspend fun recognize(imageUri: String): ReceiptRecognition =
            withContext(Dispatchers.IO) {
                val client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                var bitmap: Bitmap? = null
                var handedOff = false
                try {
                    val uri = Uri.parse(imageUri)
                    bitmap = decode(uri) ?: return@withContext ReceiptRecognition.Unreadable
                    val rotation =
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            ExifInterface(stream).rotationDegrees
                        } ?: 0
                    val ownedBitmap = checkNotNull(bitmap)
                    val task = client.process(InputImage.fromBitmap(ownedBitmap, rotation))
                    task.addOnCompleteListener(Executor { it.run() }) {
                        ownedBitmap.recycle()
                        client.close()
                    }
                    handedOff = true
                    val result = task.awaitTask()
                    val lines =
                        result.textBlocks.flatMap { it.lines }.sortedWith(
                            compareBy({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 }),
                        )
                    val draft = ReceiptParser.parse(lines.joinToString("\n") { it.text })
                    if (draft.lines.isEmpty()) ReceiptRecognition.Unreadable else ReceiptRecognition.Recognized(draft)
                } catch (_: IOException) {
                    ReceiptRecognition.Unreadable
                } catch (_: SecurityException) {
                    ReceiptRecognition.Unreadable
                } catch (_: MlKitException) {
                    ReceiptRecognition.Unreadable
                } finally {
                    if (!handedOff) {
                        bitmap?.recycle()
                        client.close()
                    }
                }
            }

        private fun decode(uri: Uri): Bitmap? {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            var sample = 1
            while (options.outWidth / sample > MAX_EDGE || options.outHeight / sample > MAX_EDGE) sample *= 2
            options.inSampleSize = sample
            options.inJustDecodeBounds = false
            return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }

        companion object {
            private const val MAX_EDGE = 4096
        }
    }
