package com.lislal.teststripmarketplace.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lislal.teststripmarketplace.viewmodel.AdminViewModel
import com.lislal.teststripmarketplace.viewmodel.AppUser

@Composable
fun UsersTab(
    adminViewModel: AdminViewModel = viewModel()
) {
    // Collect the real-time list of users from Firebase
    val users by adminViewModel.usersFlow().collectAsState(initial = emptyList())

    // A simple state holder to show action results (e.g., "Role updated", "Error updating")
    var actionResult by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Users (${users.size})",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))

        // ─── Header Row ───────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Username column (flexible width)
            Text(
                "Username",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            // New "Role" header (fixed width)
            Text(
                "Role",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(100.dp)
            )

            // Suspend header
            Text(
                "Suspend",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(80.dp)
            )
            // Ban header
            Text(
                "Ban",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(80.dp)
            )
        }
        HorizontalDivider()

        // ─── List of Users ──────────────────────────────────
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(users) { user ->
                UserRow(
                    user = user,
                    onRoleChange = { uid, selectedRole ->
                        adminViewModel.updateUserRole(uid, selectedRole) { success ->
                            actionResult = if (success) "Role updated" else "Error updating role"
                        }
                    },
                    onToggleSuspend = { uid, newValue ->
                        adminViewModel.updateUserFlag(uid, "isSuspended", newValue) { success ->
                            actionResult = if (success) "Suspend updated" else "Error updating suspend"
                        }
                    },
                    onToggleBan = { uid, newValue ->
                        adminViewModel.updateUserFlag(uid, "isBannedSuspended", newValue) { success ->
                            actionResult = if (success) "Ban updated" else "Error updating ban"
                        }
                    }
                )
                HorizontalDivider()
            }
        }

        // ─── Action Result Snackbar Text ──────────────────
        actionResult?.let { msg ->
            Spacer(Modifier.height(12.dp))
            Text(
                msg,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserRow(
    user: AppUser,
    onRoleChange: (String, String) -> Unit,
    onToggleSuspend: (String, Boolean) -> Unit,
    onToggleBan: (String, Boolean) -> Unit
) {
    // We keep state for the dropdown expansion and the currently selected role
    var roleExpanded by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(user.role.ifBlank { "guest" }) }

    // The list of possible roles an Admin can assign
    val allRoles = listOf("guest", "seller", "buyer", "admin")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ─── Username (flexible width) ─────────────────
        Text(
            text = user.username,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        // ─── Role Dropdown (fixed width) ────────────────
        Box(modifier = Modifier.width(100.dp)) {
            ExposedDropdownMenuBox(
                expanded = roleExpanded,
                onExpandedChange = { roleExpanded = !roleExpanded }
            ) {
                TextField(
                    value = selectedRole,
                    onValueChange = { /* readOnly field */ },
                    readOnly = true,
                    label = { Text("Role") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(roleExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = roleExpanded,
                    onDismissRequest = { roleExpanded = false }
                ) {
                    allRoles.forEach { roleOption ->
                        DropdownMenuItem(
                            text = { Text(roleOption) },
                            onClick = {
                                selectedRole = roleOption
                                roleExpanded = false
                                // Propagate the role change back to the ViewModel
                                onRoleChange(user.userId, roleOption)
                            }
                        )
                    }
                }
            }
        }

        // ─── Suspend Switch ─────────────────────────────
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.width(80.dp), contentAlignment = Alignment.Center) {
            Switch(
                checked = user.isSuspended,
                onCheckedChange = { onToggleSuspend(user.userId, it) }
            )
        }

        // ─── Ban Switch ─────────────────────────────────
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.width(80.dp), contentAlignment = Alignment.Center) {
            Switch(
                checked = user.isBanned,
                onCheckedChange = { onToggleBan(user.userId, it) }
            )
        }
    }
}