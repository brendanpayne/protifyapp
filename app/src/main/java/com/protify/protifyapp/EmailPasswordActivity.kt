package com.protify.protifyapp

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class EmailPasswordActivity : Activity() {

    // [START declare_auth]
    private lateinit var auth: FirebaseAuth
    // [END declare_auth]
    init {
        auth = Firebase.auth
    }
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // [START initialize_auth]
        // Initialize Firebase Auth
        auth = Firebase.auth
        // [END initialize_auth]
    }

    public override fun onStart() {
        super.onStart()
        // [START on_start_check_user]
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
        }
        // [END on_start_check_user]
    }
     fun createAccount(email: String, password: String) {
        // [START create_user_with_email]
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) {task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                } else {
                    // If sign in fails, display a message to the user
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Account Creation Failed",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }
    fun signIn(email: String, password: String): Task<AuthResult> {
        // [START sign_in_with_email]
        return auth.signInWithEmailAndPassword(email,password)
        // [END sign_in_with_email]
    }
    public fun getCurrentUser(): FirebaseUser? {
        //This is for passing the user's info to the home page
        return auth.currentUser
    }

    companion object {
        private const val TAG = "EmailPassword"
    }
}