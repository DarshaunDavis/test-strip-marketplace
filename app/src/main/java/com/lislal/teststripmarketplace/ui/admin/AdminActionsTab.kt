package com.lislal.teststripmarketplace.ui.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lislal.teststripmarketplace.viewmodel.AdminViewModel

@Composable
fun AdminActionsTab(vm: AdminViewModel) {
    Column(Modifier.fillMaxSize()) {
        Text("Admin Actions")
        // TODO: add products, add buyers, update settings, etc.
    }
}
