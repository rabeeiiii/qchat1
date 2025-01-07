package com.example.qchat

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View
import androidx.databinding.DataBindingUtil
import com.example.qchat.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var siqnUpBinding: ActivitySignUpBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var signUpAuth: FirebaseAuth
    private lateinit var name: String
    private lateinit var email: String
    private lateinit var password: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        siqnUpBinding = DataBindingUtil.setContentView(this, R.layout.activity_sign_up)
        firestore = FirebaseFirestore.getInstance()
        signUpAuth = FirebaseAuth.getInstance()

        siqnUpBinding.signUpTextToSignIn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }

        siqnUpBinding.signUpBtn.setOnClickListener {
            name = siqnUpBinding.signUpEtName.text.toString()
            email = siqnUpBinding.signUpEmail.text.toString()
            password = siqnUpBinding.signUpPassword.text.toString()

            when {
                name.isEmpty() -> Toast.makeText(this, "Enter your name, please", Toast.LENGTH_SHORT).show()
                email.isEmpty() -> Toast.makeText(this, "Enter your email, please", Toast.LENGTH_SHORT).show()
                password.isEmpty() -> Toast.makeText(this, "Enter your password, please", Toast.LENGTH_SHORT).show()
                else -> signUpUser(name, email, password)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        siqnUpBinding.signUpProgressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun signUpUser(name: String, email: String, password: String) {
        showLoading(true)

        signUpAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
            showLoading(false)
            if (it.isSuccessful) {
                val user = signUpAuth.currentUser
                val hashMap = hashMapOf(
                    "userid" to user!!.uid,
                    "username" to name,
                    "useremail" to email,
                    "status" to "default",
                    "imageUrl" to "https://www.pngarts.com/files/6/User-Avatar-in-Suit-PNG.png"
                )

                firestore.collection("Users").document(user.uid).set(hashMap)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Sign-up successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, SignInActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Firestore error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Error: ${it.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}