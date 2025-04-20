package com.example.qchat.ui.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.qchat.R

class PhotoViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_viewer)

        // Set status bar and navigation bar colors
        window.statusBarColor = Color.BLACK
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.navigationBarColor = Color.BLACK

        val photoBase64 = intent.getStringExtra("photoBase64") ?: return
        val imageView = findViewById<ImageView>(R.id.ivFullPhoto)

        // Get reference to the back button (make sure this ID matches your layout)
        val backButton: ImageView = findViewById(R.id.photobackButton)

        // Set click listener for the back button
        backButton.setOnClickListener {
            finish()  // Close the current activity
        }

        try {
            val decodedBytes = Base64.decode(photoBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }
}