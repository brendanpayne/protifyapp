package com.protify.protifyapp.features.login

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class FirebaseLoginHelper {

    // [START declare_auth]
    private val auth: FirebaseAuth = Firebase.auth
    fun createAccount(email: String, password: String): Task<AuthResult> {
        // [START create_user_with_email]
        return auth.createUserWithEmailAndPassword(email, password)
        // [END create_user_with_email]

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
}