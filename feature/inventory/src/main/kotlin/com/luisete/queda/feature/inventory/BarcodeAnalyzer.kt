package com.luisete.queda.feature.inventory

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

internal class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit,
) : ImageAnalysis.Analyzer, AutoCloseable {
    private val isProcessing = AtomicBoolean(false)
    private val isClosed = AtomicBoolean(false)

    private val options =
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_ITF,
            )
            .build()

    private val scanner = BarcodeScanning.getClient(options)

    @OptIn(ExperimentalGetImage::class)
    @Suppress("ReturnCount")
    override fun analyze(imageProxy: ImageProxy) {
        if (isClosed.get() || !isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            isProcessing.set(false)
            imageProxy.close()
            return
        }

        val image =
            try {
                InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            } catch (e: IllegalArgumentException) {
                Log.e("BarcodeAnalyzer", "Failed to setup image for processing", e)
                isProcessing.set(false)
                imageProxy.close()
                return
            }

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (isClosed.get()) return@addOnSuccessListener
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (!rawValue.isNullOrBlank()) {
                        onBarcodeDetected(rawValue)
                        break
                    }
                }
            }
            .addOnFailureListener { e ->
                if (!isClosed.get()) {
                    Log.e("BarcodeAnalyzer", "Scanner failed", e)
                }
            }
            .addOnCompleteListener {
                isProcessing.set(false)
                imageProxy.close()
            }
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            scanner.close()
        }
    }
}
