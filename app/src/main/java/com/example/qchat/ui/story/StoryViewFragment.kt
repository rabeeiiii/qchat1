package com.example.qchat.ui.story

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.CountDownTimer
import android.transition.Fade
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.qchat.R

class StoryViewFragment : Fragment(R.layout.fragment_story_view) {
    private lateinit var progressBar: ProgressBar
    private lateinit var closeButton: ImageView
    private lateinit var storyImageView: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var uploadTimeTextView: TextView
    private var countDownTimer: CountDownTimer? = null
    private val storyDuration = 5000L // Story duration in milliseconds

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enterTransition = Fade()
        exitTransition = Fade()
        progressBar = view.findViewById(R.id.storyProgressBar)
        closeButton = view.findViewById(R.id.closeStory)
        storyImageView = view.findViewById(R.id.storyImageView)
        userNameTextView = view.findViewById(R.id.userName)
        uploadTimeTextView = view.findViewById(R.id.uploadTime)

        closeButton.setOnClickListener {
            countDownTimer?.cancel()
            parentFragmentManager.popBackStack()
        }

        loadStoryDetails()

        startOrRestartStoryTimer()
    }

    private fun loadStoryDetails() {
        arguments?.let { bundle ->
            val photoUrl = bundle.getString("photoUrl") ?: ""
            val profilePicture = bundle.getString("profilePicture") ?: ""
            val userName = bundle.getString("userName") ?: ""
            val uploadTime = bundle.getString("uploadTime") ?: ""

            userNameTextView.text = userName
            uploadTimeTextView.text = uploadTime

            // Load and display the story image
            decodeAndSetImage(photoUrl, storyImageView)

            // Load and display the profile picture
            view?.let { decodeAndSetImage(profilePicture, it.findViewById(R.id.profileImage)) }
        }
    }


    private fun decodeAndSetImage(encodedImage: String, imageView: ImageView) {
        try {
            val imageBytes = Base64.decode(encodedImage, Base64.DEFAULT)
            val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            imageView.setImageBitmap(decodedImage)
        } catch (e: IllegalArgumentException) {
            Log.e("StoryViewFragment", "Base64 decoding failed", e)
        }
    }


    private fun decodeAndSetImage(encodedImage: String) {
        val imageBytes = Base64.decode(encodedImage, Base64.DEFAULT)
        val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        storyImageView.setImageBitmap(decodedImage)
    }

    private fun startOrRestartStoryTimer() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(storyDuration, 50) {
            override fun onTick(millisUntilFinished: Long) {
                val progress = ((storyDuration - millisUntilFinished).toFloat() / storyDuration * 100).toInt()
                progressBar.progress = progress
            }

            override fun onFinish() {
                progressBar.progress = 100
                parentFragmentManager.popBackStack()
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }
}
