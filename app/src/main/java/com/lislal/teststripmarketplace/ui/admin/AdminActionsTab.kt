package com.lislal.teststripmarketplace.ui.admin

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.database.*
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminActionsTab() {
    val buyersRef = remember {
        FirebaseDatabase.getInstance().getReference("buyers")
    }

    // in-memory cache
    val buyers = remember { mutableStateListOf<Pair<String, String>>() }

    // one-time load
    LaunchedEffect(buyersRef) {
        buyersRef.get().addOnSuccessListener { snap ->
            buyers.clear()
            snap.children.forEach { child ->
                child.key?.let { id ->
                    val name = child.child("name").getValue(String::class.java) ?: "Unnamed"
                    buyers += id to name
                }
            }
        }
    }

    // live updates
    DisposableEffect(buyersRef) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                buyers.clear()
                snap.children.forEach { child ->
                    child.key?.let { id ->
                        val name = child.child("name").getValue(String::class.java) ?: "Unnamed"
                        buyers += id to name
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) { /* no-op */ }
        }
        buyersRef.addValueEventListener(listener)
        onDispose { buyersRef.removeEventListener(listener) }
    }

    // UI state
    var buyerExpanded    by remember { mutableStateOf(false) }
    var selectedBuyerId  by remember { mutableStateOf<String?>(null) }
    var showAddDialog    by remember { mutableStateOf(false) }

    val categories       = listOf("Test Strips", "Devices", "Insulin")
    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    var updaterExpanded  by remember { mutableStateOf(false) }
    var selectedUpdaterId by remember { mutableStateOf<String?>(null) }

    var pickedDate       by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker   by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(Modifier.height(16.dp)) // extra space below the tabs

        // ── Manage Buyers ─────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant),
                    shape = RectangleShape
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Manage Buyers",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // ── Edit Buyer ─────────────────
                    Column(Modifier.weight(1f)) {
                        Text("Edit Buyer", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
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
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                                            buyerExpanded    = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // ── Add Buyer ──────────────────
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Add Buyer", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.size(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }

        selectedBuyerId?.let { id ->
            val name = buyers.firstOrNull { it.first == id }?.second.orEmpty()
            Text(
                "Editing: $name",
                Modifier
                    .clickable { /* TODO: EditBuyerDialog(id) */ }
                    .padding(start = 8.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // ── Categories ───────────────────────────────────────
        Text("Categories", style = MaterialTheme.typography.titleMedium)
        categories.forEach { cat ->
            Text(cat, style = MaterialTheme.typography.bodyMedium)
        }

        // ── Last Updated Dates ───────────────────────────────
        Text("Set Last Updated Date", style = MaterialTheme.typography.titleMedium)
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = !categoryExpanded }
        ) {
            OutlinedTextField(
                value = selectedCategory ?: "Select Category",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(updaterExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                                updaterExpanded   = false
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
                Button(onClick = { /* TODO */ }) {
                    Text("Set Date")
                }
            }
        }
    }

    // ── Add Buyer Dialog ───────────────────────────────────
    if (showAddDialog) {
        AddBuyerDialog(
            onDismiss = { showAddDialog = false },
            onSubmit = { buyer ->
                buyersRef.push().setValue(buyer)
                showAddDialog = false
            }
        )
    }

    // ── DatePicker ────────────────────────────────────────
    if (showDatePicker) {
        DatePickerDialog(
            initialDate    = pickedDate ?: LocalDate.now(),
            onDateSelected = {
                pickedDate    = it
                showDatePicker = false
            },
            onDismissRequest = { showDatePicker = false }
        )
    }
}

// date-picker stub – public so it resolves
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DatePickerDialog(
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
