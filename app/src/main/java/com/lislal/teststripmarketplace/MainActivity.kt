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
import com.google.firebase.database.FirebaseDatabase
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

                // Fetch user info if already logged in (fresh launch)
                LaunchedEffect(Unit) {
                    auth.currentUser?.uid?.let { uid ->
                        dbRef.child(uid).get().addOnSuccessListener { snapshot ->
                            username = snapshot.child("username").value?.toString().orEmpty()
                            userRole = snapshot.child("role").value?.toString().orEmpty()
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
