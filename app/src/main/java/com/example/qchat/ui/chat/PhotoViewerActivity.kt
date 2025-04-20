package com.example.qchat.ui.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.qchat.R

class PhotoViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_viewer)

        val photoBase64 = intent.getStringExtra("photoBase64") ?: return
        val imageView = findViewById<ImageView>(R.id.ivFullPhoto)

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