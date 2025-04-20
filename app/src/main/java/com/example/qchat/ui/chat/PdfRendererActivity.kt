package com.example.qchat.ui.chat

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.qchat.R
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream

class PdfRendererActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_renderer)

        // Set status bar and navigation bar colors
        window.statusBarColor = Color.BLACK
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.navigationBarColor = Color.BLACK

        // Set up back button
        findViewById<ImageView>(R.id.pdfbackButton).setOnClickListener {
            finish()
        }

        val documentUrl = intent.getStringExtra("documentUrl")
        val documentName = intent.getStringExtra("documentName") ?: "document.pdf"

        if (documentUrl == null) {
            Toast.makeText(this, "No document URL provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Toast.makeText(this, "Downloading document...", Toast.LENGTH_SHORT).show()

        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(documentUrl)
        val localFile = File(cacheDir, documentName)

        storageRef.getFile(localFile).addOnSuccessListener {
            renderPdf(localFile)
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Failed to download document: ${exception.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun renderPdf(file: File) {
        val container = findViewById<LinearLayout>(R.id.pdfPageContainer)

        if (!file.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fileDescriptor)

            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels - (16 * displayMetrics.density).toInt()

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)

                val scale = screenWidth.toFloat() / page.width
                val scaledWidth = screenWidth
                val scaledHeight = (page.height * scale).toInt()

                val bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)

                val transform = android.graphics.Matrix().apply {
                    postScale(scale, scale)
                }
                page.render(bitmap, null, transform, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val imageView = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, (8 * displayMetrics.density).toInt())
                    }
                    setImageBitmap(bitmap)
                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }

                container.addView(imageView)
                page.close()
            }

            renderer.close()
            fileDescriptor.close()
        } catch (e: Exception) {
            Toast.makeText(this, "Error rendering PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}