package com.scanflow.app

import android.app.Application
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import org.opencv.android.OpenCVLoader

class ScanFlowApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize PDFBox for Android offline document processing
        try {
            PDFBoxResourceLoader.init(this)
            Log.d(TAG, "PDFBox initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize PDFBox", e)
        }

        // Initialize OpenCV for offline contour & edge detection
        try {
            if (OpenCVLoader.initLocal()) {
                Log.d(TAG, "OpenCV initialized successfully")
            } else {
                Log.w(TAG, "OpenCV native library failed to load locally")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "OpenCV initialization skipped or deferred: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "ScanFlowApp"
        lateinit var instance: ScanFlowApplication
            private set
    }
}
