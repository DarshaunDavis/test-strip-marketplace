package com.lislal.teststripmarketplace.ui.home

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ForgotPasswordDialog(
    onDismiss: () -> Unit,
    onResetSent: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(context, "Enter a valid email", Toast.LENGTH_SHORT).show()
                } else {
                    FirebaseAuth.getInstance()
                        .sendPasswordResetEmail(email.trim())
                        .addOnSuccessListener {
                            Toast.makeText(context, "Reset email sent.", Toast.LENGTH_SHORT).show()
                            onResetSent()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }) {
                Text("Send Reset Email")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Reset Password") },
        text = {
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    )
}
