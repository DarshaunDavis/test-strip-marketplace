package com.lislal.teststripmarketplace.ui.home

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.lislal.teststripmarketplace.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    username: String?,
    isLoggedIn: Boolean,
    userRole: String,
    onLogin: (email: String, password: String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showPasswordResetSnackbar by remember { mutableStateOf(false) }

    var showLoginDialog by remember { mutableStateOf(false) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var forgotEmail by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isLoggedIn) "Hello, $username" else "Hello, Guest",
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            painter = painterResource(R.drawable.test_strip_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .weight(1f)
                        )
                        TextButton(
                            onClick = {
                                if (isLoggedIn) onLogout()
                                else showLoginDialog = true
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (isLoggedIn) "Logout" else "Login")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Home, contentDescription = "Home") })
                NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.AccountBox, contentDescription = "Scan") })
                NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.Person, contentDescription = "Account") })
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Role: $userRole", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(16.dp))

            Text("FILTERS + SORT OPTIONS", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            repeat(5) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        "Buyer Ad #${it + 1}",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // 🔐 Login Dialog
    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = { showLoginDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Email and password cannot be empty", Toast.LENGTH_SHORT).show()
                        } else {
                            onLogin(email.trim(), password)
                            showLoginDialog = false
                            email = ""
                            password = ""
                        }
                    }
                ) {
                    Text("Login")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoginDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Log In") },
            text = {
                Column {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = {
                            showLoginDialog = false
                            showForgotPasswordDialog = true
                        }) {
                            Text("Forgot Password?")
                        }

                        TextButton(onClick = {
                            showLoginDialog = false
                            showRegisterDialog = true
                        }) {
                            Text("Register")
                        }
                    }
                }
            }
        )
    }

    // ✅ Password Reset Snackbar
    if (showPasswordResetSnackbar) {
        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar("Password reset email sent.")
            showPasswordResetSnackbar = false
        }
    }

    // ✅ Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    if (forgotEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(forgotEmail).matches()) {
                        Toast.makeText(context, "Enter a valid email", Toast.LENGTH_SHORT).show()
                    } else {
                        FirebaseAuth.getInstance().sendPasswordResetEmail(forgotEmail.trim())
                            .addOnSuccessListener {
                                showForgotPasswordDialog = false
                                forgotEmail = ""
                                showPasswordResetSnackbar = true
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Failed to send reset email", Toast.LENGTH_SHORT).show()
                            }
                    }
                }) {
                    Text("Send Reset Email")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Reset Password") },
            text = {
                Column {
                    OutlinedTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    // 🆕 Register Dialog Placeholder
    if (showRegisterDialog) {
        AlertDialog(
            onDismissRequest = { showRegisterDialog = false },
            confirmButton = {
                TextButton(onClick = { showRegisterDialog = false }) {
                    Text("OK")
                }
            },
            title = { Text("Register") },
            text = { Text("Registration dialog coming soon.") }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        username = "Darshaun",
        isLoggedIn = true,
        userRole = "buyer",
        onLogin = { _, _ -> },
        onLogout = {}
    )
}
