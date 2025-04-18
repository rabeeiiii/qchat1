package com.example.qchat.ui.profile

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.qchat.R
import com.example.qchat.model.User
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment(R.layout.profile_fragment) {

    private lateinit var user: User
    private lateinit var userName: TextView
    private lateinit var userImage: ImageView
    private lateinit var userStatus: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.profile_fragment, container, false)

        userName = view.findViewById(R.id.username)
        userImage = view.findViewById(R.id.profileImage)
        userStatus = view.findViewById(R.id.status)
        user = arguments?.getSerializable("user") as? User ?: User("", "", "", "", "")

        loadUserProfileFromFirestore(user.id)

        val backButton: ImageView = view.findViewById(R.id.profilebackButton)
        backButton.setOnClickListener {
            val action = ProfileFragmentDirections.actionProfileFragmentToChatFragment(user)
            findNavController().navigate(action)
        }

        return view
    }

    private fun loadUserProfileFromFirestore(userId: String) {
        FirebaseFirestore.getInstance().collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: ""
                    val email = document.getString("email") ?: ""
                    val status = document.getString("status") ?: "Hey there! I'm using QChat" // Default value for status

                    userName.text = name
                    userStatus.text = status

                    val imageBase64 = document.getString("image") ?: ""
                    if (imageBase64.isNotEmpty()) {
                        val bitmap = decodeBase64ToBitmap(imageBase64)
                        userImage.setImageBitmap(bitmap)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load user profile", Toast.LENGTH_SHORT).show()
            }
    }

    fun editStatus(view: View) {
        val currentStatus = userStatus.text.toString()

        val statusEditDialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Status")
            .setMessage("Enter your new status")
            .setView(EditText(requireContext()).apply {
                setText(currentStatus)
            })
            .setPositiveButton("Save") { dialog, _ ->
                val newStatus = (dialog as AlertDialog).findViewById<EditText>(android.R.id.edit)?.text.toString()
                updateUserStatus(newStatus)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .create()

        statusEditDialog.show()
    }

    private fun updateUserStatus(newStatus: String) {
        val userId = user.id
        val updates = hashMapOf<String, Any>("status" to newStatus)

        FirebaseFirestore.getInstance().collection("users")
            .document(userId)
            .update(updates)
            .addOnSuccessListener {
                userStatus.text = newStatus
                Toast.makeText(context, "Status updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to update status", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        fun newInstance(user: User): ProfileFragment {
            val fragment = ProfileFragment()
            val bundle = Bundle()
            bundle.putSerializable("user", user)
            fragment.arguments = bundle
            return fragment
        }
    }

    private fun decodeBase64ToBitmap(encodedImage: String): Bitmap {
        val decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }
}
