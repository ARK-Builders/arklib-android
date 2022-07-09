package space.taran.arklib;

import android.graphics.Bitmap

private external fun pdfPreviewGenerateNative(data: ByteArray, quality: String): Bitmap

fun pdfPreviewGenerate(data: ByteArray, previewQuality: PreviewQuality): Bitmap {
   return pdfPreviewGenerateNative(data,previewQuality.name)
}

// Initialize Rust Library Logging
external fun initRustLogger()

enum class PreviewQuality {
    HIGH,MEDIUM,LOW
}
