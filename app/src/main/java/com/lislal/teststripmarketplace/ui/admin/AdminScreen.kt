package com.lislal.teststripmarketplace.ui.admin

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lislal.teststripmarketplace.viewmodel.AdminViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AdminScreen(
    adminViewModel: AdminViewModel = viewModel()
) {
    val tabs = listOf("Users", "Products", "Admin")
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> UsersTab(adminViewModel)
            1 -> ProductsTab()
            2 -> AdminActionsTab()
        }
    }
}
