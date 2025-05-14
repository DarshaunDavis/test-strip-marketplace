package com.lislal.teststripmarketplace.ui.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lislal.teststripmarketplace.viewmodel.AdminViewModel

@Composable
fun ProductsTab(vm: AdminViewModel) {
    Column(Modifier.fillMaxSize()) {
        Text("Products & Prices")
        // TODO: integrate PriceEditor, list products with price fields
    }
}
