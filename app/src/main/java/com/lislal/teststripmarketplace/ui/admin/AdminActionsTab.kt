package com.lislal.teststripmarketplace.ui.admin

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.database.*
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminActionsTab() {
    // — your buyers node —
    val buyersRef = remember {
        FirebaseDatabase.getInstance().getReference("buyers")
    }
    // in-memory list of (id, name)
    val buyers = remember { mutableStateListOf<Pair<String, String>>() }

    // 1) One-time load so UI isn’t empty
    LaunchedEffect(buyersRef) {
        buyersRef.get()
            .addOnSuccessListener { snap ->
                buyers.clear()
                snap.children.forEach { child ->
                    val id = child.key ?: return@forEach
                    val name = child.child("name").getValue(String::class.java) ?: "Unnamed"
                    buyers += id to name
                }
            }
    }

    // 2) Live updates
    DisposableEffect(buyersRef) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                buyers.clear()
                snap.children.forEach { child ->
                    val id = child.key ?: return@forEach
                    val name = child.child("name").getValue(String::class.java) ?: "Unnamed"
                    buyers += id to name
                }
            }
            override fun onCancelled(error: DatabaseError) { /* no-op */ }
        }
        buyersRef.addValueEventListener(listener)
        onDispose { buyersRef.removeEventListener(listener) }
    }

    // UI state
    var buyerExpanded     by remember { mutableStateOf(false) }
    var selectedBuyerId   by remember { mutableStateOf<String?>(null) }

    val categories        = listOf("Test Strips", "Devices", "Insulin")
    var categoryExpanded  by remember { mutableStateOf(false) }
    var selectedCategory  by remember { mutableStateOf<String?>(null) }

    var updaterExpanded   by remember { mutableStateOf(false) }
    var selectedUpdaterId by remember { mutableStateOf<String?>(null) }

    var pickedDate        by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker    by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ── Buyers ────────────────────────────────────────────
        Text("Manage Buyers", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(
                expanded = buyerExpanded,
                onExpandedChange = { buyerExpanded = !buyerExpanded }
            ) {
                OutlinedTextField(
                    value = buyers.firstOrNull { it.first == selectedBuyerId }?.second
                        ?: if (buyers.isEmpty()) "Loading buyers…" else "Select Buyer",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(buyerExpanded)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = buyerExpanded,
                    onDismissRequest = { buyerExpanded = false }
                ) {
                    buyers.forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                selectedBuyerId = id
                                buyerExpanded = false
                            }
                        )
                    }
                }
            }
            Button(onClick = { /* TODO: AddBuyerDialog */ }) {
                Text("Add Buyer")
            }
        }
        selectedBuyerId?.let { id ->
            val name = buyers.firstOrNull { it.first == id }?.second.orEmpty()
            Text(
                "Editing: $name",
                Modifier
                    .clickable { /* TODO: EditBuyerDialog(id) */ }
                    .padding(8.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // ── Categories ────────────────────────────────────────
        Text("Categories", style = MaterialTheme.typography.titleMedium)
        categories.forEach { cat ->
            Text(cat, style = MaterialTheme.typography.bodyMedium)
        }

        // ── Last Updated Dates ────────────────────────────────
        Text("Set Last Updated Date", style = MaterialTheme.typography.titleMedium)
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = !categoryExpanded }
        ) {
            OutlinedTextField(
                value = selectedCategory ?: "Select Category",
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
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

        selectedCategory?.let {
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = updaterExpanded,
                onExpandedChange = { updaterExpanded = !updaterExpanded }
            ) {
                OutlinedTextField(
                    value = buyers.firstOrNull { it.first == selectedUpdaterId }?.second
                        ?: "Select Buyer",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(updaterExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = updaterExpanded,
                    onDismissRequest = { updaterExpanded = false }
                ) {
                    buyers.forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                selectedUpdaterId = id
                                updaterExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (selectedUpdaterId != null) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showDatePicker = true }) {
                    Text(pickedDate?.toString() ?: "Pick Date")
                }
                Button(onClick = {
                    /* TODO: save lastUpdated[selectedCategory] = pickedDate by selectedUpdaterId */
                }) {
                    Text("Set Date")
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            initialDate    = pickedDate ?: LocalDate.now(),
            onDateSelected = {
                pickedDate = it
                showDatePicker = false
            },
            onDismissRequest = { showDatePicker = false }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title   = { Text("Select Date") },
        text    = { /* TODO: date-picker UI */ },
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
