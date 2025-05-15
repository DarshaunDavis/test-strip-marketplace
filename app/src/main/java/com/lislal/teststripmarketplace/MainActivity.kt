package com.lislal.teststripmarketplace

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.lislal.teststripmarketplace.ui.home.HomeScreen
import com.lislal.teststripmarketplace.ui.theme.TestStripMarketplaceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TestStripMarketplaceTheme {
                val context = LocalContext.current
                val auth = remember { FirebaseAuth.getInstance() }
                val dbRef = remember { FirebaseDatabase.getInstance().getReference("users") }

                var isLoggedIn by rememberSaveable { mutableStateOf(auth.currentUser != null) }
                var username   by rememberSaveable { mutableStateOf("") }
                var userRole   by rememberSaveable { mutableStateOf("") }

                // Listen for changes on the current user's profile & flags
                val uid = auth.currentUser?.uid
                DisposableEffect(uid) {
                    if (uid != null) {
                        val listener = object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                // Update profile fields
                                username = snapshot.child("username").getValue(String::class.java).orEmpty()
                                userRole = snapshot.child("role").getValue(String::class.java).orEmpty()

                                // Check suspension/ban flags
                                val suspended = snapshot.child("isSuspended")
                                    .getValue(Boolean::class.java) ?: false
                                val banned = snapshot.child("isBannedSuspended")
                                    .getValue(Boolean::class.java) ?: false

                                if (suspended || banned) {
                                    Toast.makeText(
                                        context,
                                        if (banned) "Your account has been banned." else "Your account has been suspended.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    auth.signOut()
                                    isLoggedIn = false
                                } else {
                                    isLoggedIn = true
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // Optionally handle cancellation
                            }
                        }

                        // Attach and detach listener
                        dbRef.child(uid).addValueEventListener(listener)
                        onDispose {
                            dbRef.child(uid).removeEventListener(listener)
                        }
                    } else {
                        // No logged-in user: ensure state reflects that
                        isLoggedIn = false
                        onDispose { /* nothing to clean up */ }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        username   = username,
                        isLoggedIn = isLoggedIn,
                        userRole   = userRole,
                        modifier   = Modifier.padding(innerPadding),
                        onLogin    = { email, password ->
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener { _ ->
                                    // Listener above will immediately fire with profile+flags
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Login failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        },
                        onLogout   = {
                            auth.signOut()
                            isLoggedIn = false
                            username   = ""
                            userRole   = ""
                        }
                    )
                }
            }
        }
    }
}
