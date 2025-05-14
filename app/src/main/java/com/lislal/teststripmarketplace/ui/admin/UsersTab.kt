package com.lislal.teststripmarketplace.ui.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lislal.teststripmarketplace.viewmodel.AdminViewModel

@Composable
fun UsersTab(vm: AdminViewModel) {
    Column(Modifier.fillMaxSize()) {
        Text("Users Management")
        // TODO: total users, active/banned/suspended counts, ban/unban/suspend controls
    }
}
