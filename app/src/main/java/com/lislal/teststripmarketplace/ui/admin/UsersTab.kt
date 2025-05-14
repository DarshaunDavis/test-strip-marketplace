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
    val users by adminViewModel.usersFlow().collectAsState(initial = emptyList())
    var actionResult by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Users (${users.size})", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        // ─── Header Row ───────────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Username", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text("Suspend", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.width(80.dp))
            Text("Ban", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.width(80.dp))
        }
        HorizontalDivider()

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(users) { user ->
                UserRow(
                    user = user,
                    onToggleSuspend = { uid, new ->
                        adminViewModel.updateUserFlag(uid, "isSuspended", new) { success ->
                            actionResult = if (success) "Suspend updated" else "Error updating"
                        }
                    },
                    onToggleBan = { uid, new ->
                        adminViewModel.updateUserFlag(uid, "isBannedSuspended", new) { success ->
                            actionResult = if (success) "Ban updated" else "Error updating"
                        }
                    }
                )
                HorizontalDivider()
            }
        }

        actionResult?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun UserRow(
    user: AppUser,
    onToggleSuspend: (String, Boolean) -> Unit,
    onToggleBan: (String, Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            user.username,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        // Suspend switch
        Box(modifier = Modifier.width(80.dp), contentAlignment = Alignment.Center) {
            Switch(
                checked = user.isSuspended,
                onCheckedChange = { onToggleSuspend(user.userId, it) }
            )
        }
        Spacer(Modifier.width(8.dp))
        // Ban switch
        Box(modifier = Modifier.width(80.dp), contentAlignment = Alignment.Center) {
            Switch(
                checked = user.isBanned,
                onCheckedChange = { onToggleBan(user.userId, it) }
            )
        }
    }
}
