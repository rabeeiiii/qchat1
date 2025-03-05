package com.example.qchat.ui.story

import android.os.Bundle
import android.os.CountDownTimer
import android.transition.Fade
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import com.example.qchat.R

class StoryViewFragment : Fragment(R.layout.fragment_story_view) {
    private lateinit var progressBar: ProgressBar
    private lateinit var closeButton: ImageView
    private var countDownTimer: CountDownTimer? = null
    private val storyDuration = 5000L // Story duration in milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Transition animations can be set here if using Transitions API
        // For API level 21 and above, you might use something like:
         enterTransition = Fade()
         exitTransition = Fade()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the progress bar and apply fade-in animation
        progressBar = view.findViewById(R.id.storyProgressBar)
        val fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in)
        progressBar.startAnimation(fadeInAnimation)

        // Initialize and set up the close button
        closeButton = view.findViewById(R.id.closeStory)
        closeButton.setOnClickListener {
            // Cancel the timer and remove this fragment from the view stack
            countDownTimer?.cancel()
            parentFragmentManager.popBackStack()
        }

        // Start or restart the story timer every time this view is created
        startOrRestartStoryTimer()
    }

    private fun startOrRestartStoryTimer() {
        // Cancel any existing timer
        countDownTimer?.cancel()

        // Start a new countdown timer
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
        // Cancel the timer when the view is destroyed to avoid leftover executions
        countDownTimer?.cancel()
    }
}
