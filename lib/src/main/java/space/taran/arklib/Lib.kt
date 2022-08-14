package space.taran.arklib;

import android.graphics.Bitmap

private external fun pdfPreviewGenerateNative(path: String, quality: String): Bitmap

fun pdfPreviewGenerate(path: String, previewQuality: PreviewQuality): Bitmap {
   return pdfPreviewGenerateNative(path, previewQuality.name)
}

// Initialize Rust Library Logging
external fun initRustLogger()

enum class PreviewQuality {
    HIGH,MEDIUM,LOW
}

