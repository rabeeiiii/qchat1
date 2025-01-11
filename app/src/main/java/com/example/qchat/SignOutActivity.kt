package com.example.qchat

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SignOutActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var signOutMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_out)  // Inflating the layout

        // Initialize views
        progressBar = findViewById(R.id.progressBar)
        signOutMessage = findViewById(R.id.signOutMessage)

        // Show progress and message
        progressBar.visibility = ProgressBar.VISIBLE
        signOutMessage.visibility = TextView.VISIBLE

        // Sign out the user
        signOut()
    }

    private fun signOut() {
        // Get the FirebaseAuth instance
        val auth = FirebaseAuth.getInstance()

        // Sign out the user
        auth.signOut()

        // Show a toast to inform the user
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()

        // Simulate a short delay to give the progress bar time to show
        // You can remove this delay for an immediate transition or use a handler
        android.os.Handler().postDelayed({
            // Navigate back to the SignInActivity
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)

            // Optionally, finish the current activity to clear the back stack
            finish()
        }, 1500)  // Delay of 1.5 seconds for loading animation
    }
}
