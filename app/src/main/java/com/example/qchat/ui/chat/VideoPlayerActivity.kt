package com.example.qchat.ui.chat


import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.qchat.R
import android.view.View
import android.widget.ImageView

class VideoPlayerActivity : AppCompatActivity() {
    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        window.statusBarColor = Color.BLACK
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        // Black navigation bar (optional)
        window.navigationBarColor = Color.BLACK
        val videoUrl = intent.getStringExtra("videoUrl") ?: return
        playerView = findViewById(R.id.exoPlayerView)

        // Get reference to the back button
        val backButton: ImageView = findViewById(R.id.backButton)

        // Set click listener for the back button
        backButton.setOnClickListener {
            finish()  // This will close the current activity and return to the previous one
        }

        player = ExoPlayer.Builder(this).build().also {
            playerView.player = it
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            it.setMediaItem(mediaItem)
            it.prepare()
            it.play()
        }
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }

    // Also good practice to release player in onDestroy
    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}