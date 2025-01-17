package com.example.qchat.fragments

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import androidx.fragment.app.Fragment
import com.example.qchat.R
import com.example.qchat.adapter.AttachmentAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PopMenuFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.pop_up_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gridView: GridView = view.findViewById(R.id.gridView)
        val adapter = AttachmentAdapter(
            requireContext(),
            arrayOf("Photos", "Camera", "Location"),
            arrayOf(R.drawable.gallery, R.drawable.camera, R.drawable.location)
        )
        gridView.adapter = adapter

        gridView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> openGallery()
                1 -> openCamera()
                2 -> openLocationPicker()
            }
            dismiss()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivity(intent)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivity(intent)
    }

    private fun openLocationPicker() {
    }
}
