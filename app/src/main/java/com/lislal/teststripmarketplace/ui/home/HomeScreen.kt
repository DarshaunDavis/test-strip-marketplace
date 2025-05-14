package com.lislal.teststripmarketplace.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lislal.teststripmarketplace.R
import com.lislal.teststripmarketplace.ui.admin.AdminScreen

@Composable
fun HomeScreen(
    username: String?,
    isLoggedIn: Boolean,
    userRole: String,
    onLogin: (email: String, password: String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showPasswordResetSnackbar by remember { mutableStateOf(false) }

    // ➊ Build tab list based on role
    data class Tab(val title: String, val icon: ImageVector)
    val tabs by remember(userRole) {
        mutableStateOf(
            buildList {
                add(Tab("Home", Icons.Default.Home))
                add(Tab("Scan", Icons.Default.QrCodeScanner))
                if (userRole.equals("admin", ignoreCase = true)) {
                    add(Tab("Admin", Icons.Default.AdminPanelSettings))
                }
                add(Tab("Account", Icons.Default.Person))
            }
        )
    }
    var selectedIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { idx, tab ->
                    NavigationBarItem(
                        selected = selectedIndex == idx,
                        onClick = { selectedIndex = idx },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (tabs[selectedIndex].title) {
                "Home" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        // ─── HEADER ─────────────────────────────────
                        val hour = java.util.Calendar.getInstance()
                            .get(java.util.Calendar.HOUR_OF_DAY)
                        val greetingPrefix = when (hour) {
                            in 0..11 -> "Good morning"
                            in 12..16 -> "Good afternoon"
                            else -> "Good evening"
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 8.dp)
                        ) {
                            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                                Text(greetingPrefix, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = if (isLoggedIn && !username.isNullOrBlank()) username else "Guest",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                            Icon(
                                painter = painterResource(R.drawable.test_strip_logo),
                                contentDescription = "App Logo",
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(150.dp)
                            )
                            TextButton(
                                onClick = {
                                    if (isLoggedIn) onLogout() else showLoginDialog = true
                                },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                Text(if (isLoggedIn) "Logout" else "Login")
                            }
                        }
                        // ─────────────────────────────────────────────

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Role: $userRole", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("FILTERS + SORT OPTIONS", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            placeholder = { Text("Search buyers…") },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        repeat(5) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { }
                            ) {
                                Text(
                                    text = "Buyer Ad #${it + 1}",
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                "Scan" -> ScanPlaceholder()
                "Admin" -> AdminScreen(modifier = Modifier.fillMaxSize())
                "Account" -> AccountPlaceholder()
            }
        }
    }

    // ─── DIALOGS ────────────────────────────────────
    if (showLoginDialog) {
        LoginDialog(
            onDismiss = { showLoginDialog = false },
            onLogin = onLogin,
            onForgotPasswordClick = {
                showLoginDialog = false
                showForgotPasswordDialog = true
            },
            onRegisterClick = {
                showLoginDialog = false
                showRegisterDialog = true
            }
        )
    }
    if (showRegisterDialog) {
        RegisterDialog(onDismiss = { showRegisterDialog = false }, onRegister = onLogin)
    }
    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            onDismiss = { showForgotPasswordDialog = false },
            onResetSent = {
                showForgotPasswordDialog = false
                showPasswordResetSnackbar = true
            }
        )
    }
    if (showPasswordResetSnackbar) {
        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar("Password reset email sent.")
            showPasswordResetSnackbar = false
        }
    }
}

@Composable
private fun ScanPlaceholder() = Box(
    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
) {
    Text("Scan Screen", style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun AccountPlaceholder() = Box(
    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
) {
    Text("Account Screen", style = MaterialTheme.typography.titleLarge)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        username = "AdminUser",
        isLoggedIn = true,
        userRole = "admin",
        onLogin = { _, _ -> },
        onLogout = {}
    )
}
