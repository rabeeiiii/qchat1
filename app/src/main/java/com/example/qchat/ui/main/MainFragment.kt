package com.example.qchat.ui.main

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.qchat.R
import com.example.qchat.adapter.RecentConversationsAdapter
import com.example.qchat.databinding.MainFragmentBinding
import com.example.qchat.model.Story
import com.example.qchat.ui.registration.RegistrationActivity
import com.example.qchat.utils.Constant
import com.example.qchat.utils.decodeToBitmap
import com.example.qchat.utils.toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Locale

@AndroidEntryPoint
class MainFragment : Fragment(R.layout.main_fragment) {

    private lateinit var binding: MainFragmentBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: RecentConversationsAdapter
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private var loggedInUserStory: Story? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = MainFragmentBinding.bind(view)

        clickListener()
        updateDetails()
        setRecyclerview()
        setupGalleryLauncher()
        setupStories()
        setupProfileClickListener()

        viewModel.recentMessageEventListener(adapter.getRecentList()) {
            adapter.updateRecentConversion(it)
            binding.rvRecentConversation.visibility = View.VISIBLE
            binding.pb.visibility = View.GONE
            binding.rvRecentConversation.smoothScrollToPosition(0)
        }

    }
    private fun setupGalleryLauncher() {
        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                processImage(it)
            }
        }
    }

    private fun processImage(uri: Uri) {
        lifecycleScope.launch {
            val encodedImage = encodeImage(uri)
            viewModel.sendPhotoToStories(encodedImage)
        }
    }

    private suspend fun encodeImage(uri: Uri): String = withContext(Dispatchers.IO) {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }
    private fun setupStories() {
        viewModel.fetchStories()
        viewModel.storiesLiveData.observe(viewLifecycleOwner) { stories ->
            updateStoriesUI(stories)
        }
    }

    private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: IllegalArgumentException) {
            Log.e("MainFragment", "Base64 decoding failed", e)
            null
        }
    }
    private fun updateStoriesUI(stories: List<Story>) {
        val inflater = LayoutInflater.from(context)
        val container = binding.statusBar.findViewById<LinearLayout>(R.id.storyContainer)
        val loggedInUserId = viewModel.getUserId()

        while (container.childCount > 1) {
            container.removeViewAt(container.childCount - 1)
        }

        stories.forEach { story ->
            val storyView = inflater.inflate(R.layout.story_item, container, false)
            val imageView = storyView.findViewById<ImageView>(R.id.ivStory)
            val textView = storyView.findViewById<TextView>(R.id.tvStoryName)

            decodeBase64ToBitmap(story.userProfilePicture)?.let {
                imageView.setImageBitmap(it)
            }

            textView.text = story.userName.split(" ").first()

            imageView.setOnClickListener {
                navigateToStoryView(story)
            }

            if (story.userId == loggedInUserId) {
                binding.ivProfile.setOnClickListener {
                    navigateToStoryView(story)
                }
            } else {
                container.addView(storyView)
            }
        }
    }



    private fun navigateToStoryView(story: Story) {
        activity?.findViewById<BottomNavigationView>(R.id.bottomNavigation)?.visibility = View.GONE
        val bundle = Bundle().apply {
            putString("photoUrl", story.photo)
            putString("profilePicture", story.userProfilePicture)
            putString("userName", story.userName.split(" ").first())
            putString("uploadTime", SimpleDateFormat("h:m a", Locale.getDefault()).format(story.timestamp.toDate()))
        }
        findNavController().navigate(R.id.storyViewFragment, bundle)
    }




    private fun setupProfileClickListener() {
        binding.ivProfile.setOnClickListener {
            loggedInUserStory?.let { story ->
                navigateToStoryView(story)
            }
        }
    }

    private fun updateDetails() {
        binding.ivProfile.setImageBitmap(viewModel.loadUserDetails().decodeToBitmap())

    }

    private fun clickListener() {
        binding.ivedit.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_usersFragment)
        }
        binding.ivMore.setOnClickListener {
            showMoreMenu()
        }

        binding.newstory.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

    }


    private fun setRecyclerview() {
        adapter = RecentConversationsAdapter()
        binding.rvRecentConversation.apply {
            setHasFixedSize(true)
            adapter = this@MainFragment.adapter
        }
        adapter.onClickConversation = { user ->
            val bundle = Bundle()
            bundle.putSerializable(Constant.KEY_USER, user)
            findNavController().navigate(R.id.action_mainFragment_to_chatFragment, bundle)
        }
    }

    private fun signOut() {

        viewModel.signOut().observe(viewLifecycleOwner) {
            if (it) {
                requireContext().toast("SignOut")
                val intent = Intent(requireActivity(), RegistrationActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)

            } else {
                requireContext().toast("Sign Out Successfully")
            }
        }


    }

    fun showMoreMenu() {
        val moreMenu = PopupMenu(requireContext(), binding.ivMore)
        moreMenu.inflate(R.menu.menu_more)
        moreMenu.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_sign_out){
                signOut()
                return@setOnMenuItemClickListener true
            }
            false
        }
        moreMenu.show()
    }

}