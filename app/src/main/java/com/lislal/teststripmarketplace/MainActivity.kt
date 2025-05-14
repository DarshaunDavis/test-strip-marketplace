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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.lislal.teststripmarketplace.ui.home.HomeScreen
import com.lislal.teststripmarketplace.ui.theme.TestStripMarketplaceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TestStripMarketplaceTheme {
                val auth = remember { FirebaseAuth.getInstance() }
                val dbRef = remember { FirebaseDatabase.getInstance().getReference("users") }

                var isLoggedIn by rememberSaveable { mutableStateOf(auth.currentUser != null) }
                var username by rememberSaveable { mutableStateOf("") }
                var userRole by rememberSaveable { mutableStateOf("") }
                var isSuspended by rememberSaveable { mutableStateOf(false) }
                var isBanned by rememberSaveable { mutableStateOf(false) }

                // Fetch user info if already logged in (fresh launch)
                LaunchedEffect(Unit) {
                    auth.currentUser?.uid?.let { uid ->
                        dbRef.child(uid).get().addOnSuccessListener { snapshot ->
                            username = snapshot.child("username").value?.toString().orEmpty()
                            userRole = snapshot.child("role").value?.toString().orEmpty()
                        }
                    }
                }

                // Only attach listener when logged in
                auth.currentUser?.uid?.let { uid ->
                    DisposableEffect(uid) {
                        val listener = dbRef.child(uid).addValueEventListener(object :
                            ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                username = snapshot.child("username").getValue(String::class.java).orEmpty()
                                userRole = snapshot.child("role").getValue(String::class.java).orEmpty()
                                isSuspended = snapshot.child("isSuspended").getValue(Boolean::class.java) ?: false
                                isBanned = snapshot.child("isBannedSuspended").getValue(Boolean::class.java) ?: false
                                // If user is banned or suspended, you can immediately force logout or disable UI here
                                if (isBanned || isSuspended) {
                                    auth.signOut()
                                    isLoggedIn = false
                                }
                            }
                            override fun onCancelled(error: DatabaseError) { /* handle error */ }
                        })
                        onDispose {
                            dbRef.child(uid).removeEventListener(listener)
                        }
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
                                    dbRef.child(uid).get().addOnSuccessListener { snapshot ->
                                        username = snapshot.child("username").value?.toString().orEmpty()
                                        userRole = snapshot.child("role").value?.toString().orEmpty()
                                        isLoggedIn = true
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Login failed: ${it.message}", Toast.LENGTH_SHORT).show()
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
