package com.lislal.teststripmarketplace

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.lislal.teststripmarketplace.ui.home.HomeScreen
import com.lislal.teststripmarketplace.ui.theme.TestStripMarketplaceTheme
import com.lislal.teststripmarketplace.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current

            val isLoggedIn = authViewModel.isLoggedIn.collectAsState().value
            val userEmail = authViewModel.userEmail.collectAsState().value
            val userRole = authViewModel.userRole.collectAsState().value

            TestStripMarketplaceTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        username = userEmail.ifBlank { "Guest" },
                        isLoggedIn = isLoggedIn,
                        userRole = userRole,
                        onLogin = { email, password ->
                            authViewModel.login(email, password) { success ->
                                if (!success) {
                                    Toast.makeText(context, "Login failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onLogout = { authViewModel.logout() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
