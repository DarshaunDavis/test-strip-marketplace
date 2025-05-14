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
                var username by rememberSaveable { mutableStateOf("") }
                var userRole by rememberSaveable { mutableStateOf("") }
                var isSuspended by rememberSaveable { mutableStateOf(false) }
                var isBanned by rememberSaveable { mutableStateOf(false) }

                // On fresh launch: fetch profile + flags
                LaunchedEffect(Unit) {
                    auth.currentUser?.uid?.let { uid ->
                        dbRef.child(uid).get()
                            .addOnSuccessListener { snap ->
                                username = snap.child("username").value?.toString().orEmpty()
                                userRole = snap.child("role").value?.toString().orEmpty()
                                isSuspended = snap.child("isSuspended").getValue(Boolean::class.java) ?: false
                                isBanned    = snap.child("isBannedSuspended").getValue(Boolean::class.java) ?: false

                                if (isSuspended || isBanned) {
                                    val msg = if (isBanned) "Your account has been banned." else "Your account has been suspended."
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    auth.signOut()
                                    isLoggedIn = false
                                }
                            }
                    }
                }

                // Listen for live changes to flags and profile
                auth.currentUser?.uid?.let { uid ->
                    DisposableEffect(uid) {
                        val listener = dbRef.child(uid)
                            .addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    username = snapshot.child("username").getValue(String::class.java).orEmpty()
                                    userRole = snapshot.child("role").getValue(String::class.java).orEmpty()
                                    val suspended = snapshot.child("isSuspended").getValue(Boolean::class.java) ?: false
                                    val banned    = snapshot.child("isBannedSuspended").getValue(Boolean::class.java) ?: false

                                    if (suspended || banned) {
                                        val msg = if (banned) "You have been banned." else "You have been suspended."
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        auth.signOut()
                                        isLoggedIn = false
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) { /* ignore */ }
                            })
                        onDispose { dbRef.child(uid).removeEventListener(listener) }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        username = username,
                        isLoggedIn = isLoggedIn,
                        userRole = userRole,
                        modifier = Modifier.padding(innerPadding),
                        onLogin = { email, password ->
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener { result ->
                                    val uid = result.user?.uid ?: return@addOnSuccessListener
                                    // Fetch profile + suspension/ban
                                    dbRef.child(uid).get()
                                        .addOnSuccessListener { snap ->
                                            val suspended = snap.child("isSuspended").getValue(Boolean::class.java) ?: false
                                            val banned    = snap.child("isBannedSuspended").getValue(Boolean::class.java) ?: false

                                            if (suspended || banned) {
                                                val msg = if (banned) "Login denied: account banned." else "Login denied: account suspended."
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                auth.signOut()
                                                isLoggedIn = false
                                            } else {
                                                username = snap.child("username").value?.toString().orEmpty()
                                                userRole = snap.child("role").value?.toString().orEmpty()
                                                isLoggedIn = true
                                            }
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "Failed to load profile.", Toast.LENGTH_SHORT).show()
                                        }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Login failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        },
                        onLogout = {
                            auth.signOut()
                            isLoggedIn = false
                            username = ""
                            userRole = ""
                        }
                    )
                }
            }
        }
    }
}
