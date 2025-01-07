package com.example.qchat

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.example.qchat.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException

class SignInActivity : AppCompatActivity() {
    private lateinit var email: String
    private lateinit var password: String
    private lateinit var signInbinding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var progressDialogSignIn: ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        signInbinding = DataBindingUtil.setContentView(this, R.layout.activity_sign_in)

        auth = FirebaseAuth.getInstance()

//        if (auth.currentUser!=null){
//
//            startActivity(Intent(this, MainActivity::class.java))
//
//        }

        progressDialogSignIn = ProgressDialog(this)

        signInbinding.signInTextToSignUp.setOnClickListener {

            startActivity(Intent(this, SignUpActivity::class.java))


        }


        signInbinding.loginButton.setOnClickListener {

            email = signInbinding.loginetemail.text.toString()
            password = signInbinding.loginetpassword.text.toString()

            if (signInbinding.loginetemail.text.isEmpty()){

                Toast.makeText(this, "Enter Email", Toast.LENGTH_SHORT).show()
            }
            if (signInbinding.loginetpassword.text.isEmpty()){

                Toast.makeText(this, "Enter Password", Toast.LENGTH_SHORT).show()

            }


            if (signInbinding.loginetemail.text.isNotEmpty() && signInbinding.loginetpassword.text.isNotEmpty()){
                signIn(password, email)
            }

        }
    }

    private fun signIn(password: String, email: String) {
        progressDialogSignIn.show()
        progressDialogSignIn.setMessage("Signing In")

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {


            if (it.isSuccessful){

                progressDialogSignIn.dismiss()
                startActivity(Intent(this, MainActivity::class.java))


            } else {

                progressDialogSignIn.dismiss()
                Toast.makeText(applicationContext, "Invalid Credentials", Toast.LENGTH_SHORT).show()
            }


        }.addOnFailureListener {exception->


            when (exception){

                is FirebaseAuthInvalidCredentialsException ->{

                    Toast.makeText(applicationContext, "Invalid Credentials", Toast.LENGTH_SHORT).show()


                }

                else-> {

                    // other exceptions
                    Toast.makeText(applicationContext, "Auth Failed", Toast.LENGTH_SHORT).show()


                }

            }
        }

    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()

        progressDialogSignIn.dismiss()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        progressDialogSignIn.dismiss()

    }

}
