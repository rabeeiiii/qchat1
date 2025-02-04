package com.example.qchat.ui.registration.signup

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.qchat.ui.main.MainActivity
import com.example.qchat.R
import com.example.qchat.databinding.SignUpFragmentBinding
import com.example.qchat.utils.*
import dagger.hilt.android.AndroidEntryPoint
import java.io.FileNotFoundException
import java.util.regex.Pattern

@AndroidEntryPoint
class SignUpFragment : Fragment(R.layout.sign_up_fragment) {

    private val viewModel: SignUpViewModel by viewModels()
    lateinit var binding: SignUpFragmentBinding
    private var encodedImage: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = SignUpFragmentBinding.bind(view)

        setClickListener()
        setObserver()
    }

    private fun setObserver() {
        viewModel.signUp.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    loading(false)
                    requireContext().toast("Registered Successfully!")
                    startActivity(Intent(requireActivity(), MainActivity::class.java))
                }
                is Resource.Error -> {
                    loading(false)
                    resource.message?.let { requireContext().toast(it) }
                }
                is Resource.Loading -> loading(true)
                is Resource.Empty -> TODO()
            }
        }
    }

    private fun setClickListener() {
        binding.tvSignIn.setOnClickListener { findNavController().popBackStack() }
        binding.btnSignUp.setOnClickListener { if (isValidSignUpDetails()) signUp() }
        binding.ivProfile.setOnClickListener {
            val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImage.launch(pickImageIntent)
        }
    }

    private fun signUp() {
        viewModel.signUp(
            binding.etName.getFiledText(),
            binding.etEmail.getFiledText(),
            binding.etPassword.getFiledText(),
            encodedImage!!
        )
    }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { it ->
        if (it.resultCode == RESULT_OK) {
            it.data?.let {
                val imageUri = it.data
                imageUri?.let {
                    try {
                        val inputStream = requireContext().contentResolver.openInputStream(imageUri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        binding.ivProfile.setImageBitmap(bitmap)
                        binding.tvAddImage.gone()
                        encodedImage = bitmap.encodeToBase64()
                    } catch (e: FileNotFoundException) {
                        requireContext().toast("Failed to fetch image")
                    }
                }
            }
        }
    }

    private fun isValidSignUpDetails(): Boolean {
        val name = binding.etName.getFiledText().trim()
        val email = binding.etEmail.getFiledText().trim()
        val password = binding.etPassword.getFiledText()
        val confirmPassword = binding.etConfirmPassword.getFiledText()

        if (encodedImage == null) {
            requireContext().toast("Please Select Profile Picture")
            return false
        }
        if (name.isEmpty() || name.length < 3) {
            requireContext().toast("Name must be at least 3 characters long")
            return false
        }
        if (email.isEmpty() || !email.isValidEmail()) {
            requireContext().toast("Please Enter a Valid Email")
            return false
        }
        if (password.isEmpty()) {
            requireContext().toast("Please Enter a Password")
            return false
        }
        if (password.length < 8) {
            requireContext().toast("Password must be at least 8 characters long")
            return false
        }
        if (!password.contains(Regex("[A-Z]"))) {
            requireContext().toast("Password must contain at least one uppercase letter")
            return false
        }
        if (!password.contains(Regex("[a-z]"))) {
            requireContext().toast("Password must contain at least one lowercase letter")
            return false
        }
        if (!password.contains(Regex("[0-9]"))) {
            requireContext().toast("Password must contain at least one number")
            return false
        }
        if (!password.contains(Regex("[@\$!%*?&]"))) {
            requireContext().toast("Password must contain at least one special character")
            return false
        }
        if (confirmPassword.isEmpty()) {
            requireContext().toast("Please Confirm Your Password")
            return false
        }
        if (password != confirmPassword) {
            requireContext().toast("Passwords do not match")
            return false
        }

        return true
    }


    private fun isPasswordStrong(password: String): Boolean {
        val passwordPattern =
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$"
        return Pattern.compile(passwordPattern).matcher(password).matches()
    }

    private fun loading(isLoading: Boolean) {
        if (isLoading) {
            binding.btnSignUp.invisible()
            binding.pbSignUp.visible()
        } else {
            binding.btnSignUp.visible()
            binding.pbSignUp.gone()
        }
    }
}
