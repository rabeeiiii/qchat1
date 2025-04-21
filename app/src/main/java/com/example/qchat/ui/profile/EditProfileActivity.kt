package com.example.qchat.ui.profile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.qchat.R
import com.example.qchat.databinding.ActivityEditProfileBinding
import com.example.qchat.repository.MainRepository
import com.example.qchat.utils.Constant
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.mindrot.jbcrypt.BCrypt
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream
import javax.inject.Inject

@AndroidEntryPoint
class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding

    @Inject
    lateinit var fireStore: FirebaseFirestore

    @Inject
    lateinit var mainRepository: MainRepository

    private var imageUri: Uri? = null
    private var encodedImage: String? = null

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    imageUri = uri
                    Glide.with(this)
                        .load(uri)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(binding.ivProfileImage)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = resources.getColor(R.color.black, theme)
        loadUserData()

        binding.btnChangePhoto.setOnClickListener {
            openImagePicker()
        }

        binding.btnSave.setOnClickListener {
            saveProfileChanges()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadUserData() {
        val userId = intent.getStringExtra("USER_ID") ?: return
        val DEFAULT_STATUS = "Just joined QChat — let's talk!"

        fireStore.collection(Constant.KEY_COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString(Constant.KEY_NAME) ?: ""
                    val email = document.getString(Constant.KEY_EMAIL) ?: ""
                    val imageBase64 = document.getString(Constant.KEY_IMAGE) ?: ""
                    val status = document.getString(Constant.KEY_STATUS) ?: DEFAULT_STATUS
                    binding.etName.setText(name)
                    binding.etEmail.setText(email)
                    binding.etstatus.setText(status)

                    if (imageBase64.isNotEmpty()) {
                        val bitmap = decodeBase64ToBitmap(imageBase64)
                        binding.ivProfileImage.setImageBitmap(bitmap)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun saveProfileChanges() {
        val userId = intent.getStringExtra("USER_ID") ?: return
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val currentPassword = binding.etCurrentPassword.text.toString().trim()
        val newPassword = binding.etNewPassword.text.toString().trim()
        val newstatus = binding.etstatus.text.toString().trim()

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (imageUri != null) {
            encodedImage = encodeImageToBase64(imageUri!!)
        }

        fireStore.collection(Constant.KEY_COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val updates = hashMapOf<String, Any>(
                        Constant.KEY_NAME to name,
                        Constant.KEY_EMAIL to email,
                        Constant.KEY_STATUS to newstatus
                    )

                    if (!encodedImage.isNullOrEmpty()) {
                        updates[Constant.KEY_IMAGE] = encodedImage!!
                    }

                    if (currentPassword.isNotEmpty() && newPassword.isNotEmpty()) {
                        val storedHashedPassword = document.getString(Constant.KEY_PASSWORD) ?: ""

                        if (BCrypt.checkpw(currentPassword, storedHashedPassword)) {
                            val newHashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())
                            updates[Constant.KEY_PASSWORD] = newHashedPassword
                        } else {
                            Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }
                    }

                    lifecycleScope.launch {
                        val isUpdated = mainRepository.updateUserProfile(userId, updates)
                        if (isUpdated) {
                            Toast.makeText(
                                this@EditProfileActivity,
                                "Profile updated successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        } else {
                            Toast.makeText(
                                this@EditProfileActivity,
                                "Failed to update profile",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to verify current password", Toast.LENGTH_SHORT).show()
            }
    }

    /** Convert selected image to Base64 with compression */
    private fun encodeImageToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream) // Compress quality to 70%
            val byteArray: ByteArray = outputStream.toByteArray()

            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            Toast.makeText(this, "Error encoding image", Toast.LENGTH_SHORT).show()
            null
        }
    }

    /** Decode Base64 string to Bitmap */
    private fun decodeBase64ToBitmap(encodedImage: String): Bitmap {
        val decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }
}