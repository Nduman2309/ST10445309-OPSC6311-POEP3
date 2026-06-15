package com.example.budgetapplication

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetapplication.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth = FirebaseAuth.getInstance()
    private val handler = Handler(Looper.getMainLooper())
    private var tickRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // CLICK_HANDLERS
        binding.registerButton.setOnClickListener { registerUser() }
        binding.goToLogin.setOnClickListener { finish() }
        
        // Accessing buttons inside the included registrationPopup
        binding.registrationPopup.btnDismissPopup.setOnClickListener { hidePopup() }
        binding.registrationPopup.btnPopupLogin.setOnClickListener { 
            hidePopup()
            finish() 
        }
    }

    private fun registerUser() {
        val fName = binding.etFirstName.text.toString().trim()
        val lName = binding.etLastName.text.toString().trim()
        val incomeStr = binding.etInitialIncome.text.toString().trim()
        val income = incomeStr.toDoubleOrNull() ?: 0.0
        
        // Corrected snake_case to camelCase for ViewBinding
        val email = binding.emailAddress.text.toString().trim()
        val password = binding.createPassword.text.toString().trim()
        val confirm = binding.confirmPassword.text.toString().trim()

        if (fName.isEmpty() || lName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "System Parameters Missing: Please complete all fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (password != confirm) {
            Toast.makeText(this, "Passwords mismatch: Please ensure both passwords match", Toast.LENGTH_SHORT).show()
            return
        }

        // FIREBASE_REGISTRATION
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val user = mapOf(
                        "uid" to uid,
                        "firstName" to fName,
                        "lastName" to lName,
                        "email" to email,
                        "totalSavings" to 0.0,
                        "initialIncomeBaseline" to income
                    )
                    FirebaseDatabase.getInstance().getReference("users").child(uid).setValue(user)
                        .addOnSuccessListener { showSuccessPopup() }
                } else {
                    val exception = task.exception
                    val message = when (exception) {
                        is FirebaseAuthUserCollisionException -> "Registration Failed: This email is already associated with an account. Please log in instead."
                        else -> "Registration Denied: ${exception?.localizedMessage}"
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun showSuccessPopup() {
        // Accessing the included layout's root and views
        binding.registrationPopup.registrationPopupOverlay.visibility = View.VISIBLE
        binding.registrationPopup.registrationPopupOverlay.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in))
        startTickLoop()
    }

    private fun startTickLoop() {
        val anim = AnimationUtils.loadAnimation(this, R.anim.tick_loop)
        tickRunnable = object : Runnable {
            override fun run() {
                binding.registrationPopup.ivTickLoop.startAnimation(anim)
                handler.postDelayed(this, 3000)
            }
        }
        handler.post(tickRunnable!!)
    }

    private fun hidePopup() {
        binding.registrationPopup.registrationPopupOverlay.visibility = View.GONE
        tickRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        tickRunnable?.let { handler.removeCallbacks(it) }
    }
}
