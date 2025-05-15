package com.lislal.teststripmarketplace.ui.admin

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminActionsTab() {
    // Placeholder data: replace with real DB/VM hooks
    val buyers = listOf("Buyer A", "Buyer B", "Buyer C")
    val categories = listOf("Test Strips", "Devices", "Insulin")

    // BUYER SELECTION
    var buyerExpanded by remember { mutableStateOf(false) }
    var selectedBuyer by remember { mutableStateOf<String?>(null) }

    // LAST UPDATED FLOW
    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    var updaterExpanded by remember { mutableStateOf(false) }
    var selectedUpdater by remember { mutableStateOf<String?>(null) }

    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ── Buyers ────────────────────────────────────────────────────
        Text("Manage Buyers", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(
                expanded = buyerExpanded,
                onExpandedChange = { buyerExpanded = !buyerExpanded }
            ) {
                TextField(
                    value = selectedBuyer ?: "Select Buyer",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(buyerExpanded) },
                    modifier = Modifier.weight(1f)
                )
                ExposedDropdownMenu(
                    expanded = buyerExpanded,
                    onDismissRequest = { buyerExpanded = false }
                ) {
                    buyers.forEach { buyer ->
                        DropdownMenuItem(
                            text = { Text(buyer) },
                            onClick = {
                                selectedBuyer = buyer
                                buyerExpanded = false
                            }
                        )
                    }
                }
            }
            Button(onClick = { /* TODO: open AddBuyerDialog() */ }) {
                Text("Add Buyer")
            }
        }
        selectedBuyer?.let { buyer ->
            Text(
                "Editing: $buyer",
                modifier = Modifier
                    .clickable { /* TODO: open EditBuyerDialog(buyer) */ }
                    .padding(8.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // ── Categories ────────────────────────────────────────────────
        Text("Categories", style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            categories.forEach { cat ->
                Text(cat, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // ── Last Updated Dates ────────────────────────────────────────
        Text("Set Last Updated Date", style = MaterialTheme.typography.titleMedium)
        // 1) Choose category
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = !categoryExpanded }
        ) {
            TextField(
                value = selectedCategory ?: "Select Category",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                modifier = Modifier.fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = {
                            selectedCategory = cat
                            categoryExpanded = false
                        }
                    )
                }
            }
        }
        // 2) Once category chosen, pick updater
        selectedCategory?.let {
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = updaterExpanded,
                onExpandedChange = { updaterExpanded = !updaterExpanded }
            ) {
                TextField(
                    value = selectedUpdater ?: "Select Buyer",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(updaterExpanded) },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = updaterExpanded,
                    onDismissRequest = { updaterExpanded = false }
                ) {
                    buyers.forEach { buyer ->
                        DropdownMenuItem(
                            text = { Text(buyer) },
                            onClick = {
                                selectedUpdater = buyer
                                updaterExpanded = false
                            }
                        )
                    }
                }
            }
        }
        // 3) Once updater chosen, show date picker + button
        if (selectedUpdater != null) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { showDatePicker = true }) {
                    Text(pickedDate?.toString() ?: "Pick Date")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    /* TODO: save lastUpdated[selectedCategory] = pickedDate by selectedUpdater */
                }) {
                    Text("Set Date")
                }
            }
        }
    }

    // DatePickerDialog placeholder
    if (showDatePicker) {
        DatePickerDialog(
            initialDate = pickedDate ?: LocalDate.now(),
            onDateSelected = {
                pickedDate = it
                showDatePicker = false
            },
            onDismissRequest = { showDatePicker = false }
        )
    }
}

// Stub of a date picker dialog
@Composable
private fun DatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Date") },
        text = { /* TODO: platform-specific date picker UI */ },
        confirmButton = {
            TextButton(onClick = { onDateSelected(initialDate) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
