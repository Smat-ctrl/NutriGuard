package k.nutriguard.ui.scanner

import android.content.Context
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.text.SimpleDateFormat
import java.util.Locale

// Combine/Merged 2 codes from tutorials using Android CameraX and Google's ML KIT, links below:
// https://developer.android.com/codelabs/camerax-getting-started#4 : Android
// https://developers.google.com/ml-kit/vision/barcode-scanning/android#try-it-out : Google ML Kit


// Format used to generate a unique filename for each captured image
private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
private const val TAG = "BarcodeScan"

fun takePhotoAndScanBarcode(
    context: Context,
    imageCapture: ImageCapture,
    onBarcodeScanned: (String) -> Unit
) {
    //create a time stamped filename
    val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
        .format(System.currentTimeMillis())

    // Metadata describing how and where the image will be stored
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name) // visible name in gallery
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg") // JPEG image
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Barcode")
        }
    }

    // Configure ImageCapture to write into MediaStore using the metadata above
    val outputOptions = ImageCapture.OutputFileOptions
        .Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        .build()

    // trigger the actual picture capture
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            // called if the capture fails
            override fun onError(exc: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
            }

            // called when the images are successfully read into MediaStore
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = output.savedUri
                if (savedUri == null) {
                    Log.e(TAG, "No URI returned from ImageCapture")
                    return
                }

                try {
                    // wrap the saved file as an ML Kit Input Image
                    val image = InputImage.fromFilePath(context, savedUri)

                    // ML Kit barcode scanner
                    val scanner = BarcodeScanning.getClient()

                    // process the image to detect barcodes
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            // Get the first barcode found if any
                            val first = barcodes.firstOrNull()
                            val rawValue = first?.rawValue
                            if (rawValue != null) {
                                // pass the decoded string to the caller
                                onBarcodeScanned(rawValue)
                            } else {
                                Log.d(TAG, "No barcode found in image")
                            }
                        }
                        .addOnFailureListener { e ->
                            // something went wrong with ML Kit
                            Log.e(TAG, "Barcode scan failed", e)
                        }
                } catch (e: Exception) {
                    // Error creating InputImage like file isn't readable
                    Log.e(TAG, "Failed to create InputImage from URI", e)
                }
            }
        }
    )
}
