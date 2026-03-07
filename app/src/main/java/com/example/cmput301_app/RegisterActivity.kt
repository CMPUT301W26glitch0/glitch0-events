package com.example.cmput301_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val registerMain = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.register_main)
        ViewCompat.setOnApplyWindowInsetsListener(registerMain) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etName = findViewById<EditText>(R.id.et_name)
        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val btnRegister = findViewById<Button>(R.id.btn_register)
        val tvLoginLink = findViewById<TextView>(R.id.tv_login_link)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password should be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnRegister.isEnabled = false

            // 1. Create User in Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid
                        
                        if (uid != null) {
                            val userMap = hashMapOf(
                                "name" to name,
                                "email" to email,
                                "role" to "organizer"
                            )
                            
                            // 2. Save to Firestore
                            db.collection("users").document(uid)
                                .set(userMap)
                                .addOnCompleteListener { firestoreTask ->
                                    // 3. Navigate to Dashboard using the Activity context
                                    if (firestoreTask.isSuccessful) {
                                        Toast.makeText(this@RegisterActivity, "Welcome, $name!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@RegisterActivity, "Account created, but profile error: ${firestoreTask.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                    
                                    val intent = Intent(this@RegisterActivity, DashboardActivity::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    startActivity(intent)
                                    finish()
                                }
                        } else {
                            // Fallback if uid is null
                            val intent = Intent(this@RegisterActivity, DashboardActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        btnRegister.isEnabled = true
                        Toast.makeText(this, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvLoginLink.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}