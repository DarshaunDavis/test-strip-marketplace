package com.lislal.teststripmarketplace.ui.admin

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
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
    // ── Firebase references ───────────────────────────────
    val buyersRef     = remember { FirebaseDatabase.getInstance().getReference("buyers") }
    val categoriesRef = remember { FirebaseDatabase.getInstance().getReference("categories") }

    // ── In-memory caches ──────────────────────────────────
    val buyers     = remember { mutableStateListOf<Pair<String, String>>() }
    val categories = remember { mutableStateListOf<String>() }

    // ── Buyers: one-time load + live updates ─────────────
    LaunchedEffect(buyersRef) {
        buyersRef.get().addOnSuccessListener { snap ->
            buyers.clear()
            snap.children.forEach { child ->
                child.key?.let { id ->
                    val name = child.child("name")
                        .getValue(String::class.java) ?: "Unnamed"
                    buyers += id to name
                }
            }
        }
    }
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
            override fun onCancelled(error: DatabaseError) {}
        }
        buyersRef.addValueEventListener(listener)
        onDispose { buyersRef.removeEventListener(listener) }
    }

    // ── Categories: one-time load + live updates ──────────
    LaunchedEffect(categoriesRef) {
        categoriesRef.get().addOnSuccessListener { snap ->
            categories.clear()
            snap.children.forEach { it.key?.let(categories::add) }
        }
    }
    DisposableEffect(categoriesRef) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                categories.clear()
                snap.children.forEach { it.key?.let(categories::add) }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        categoriesRef.addValueEventListener(listener)
        onDispose { categoriesRef.removeEventListener(listener) }
    }

    // ── UI state ───────────────────────────────────────────
    var buyerExpanded      by remember { mutableStateOf(false) }
    var selectedBuyerId    by remember { mutableStateOf<String?>(null) }
    var showAddBuyer       by remember { mutableStateOf(false) }
    var showEditBuyer      by remember { mutableStateOf(false) }

    var showAddCategory    by remember { mutableStateOf(false) }
    var showDeleteCategory by remember { mutableStateOf(false) }

    var pickedDate         by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker     by remember { mutableStateOf(false) }

    // ── Main scrollable column ────────────────────────────
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // ── Manage Buyers Card ───────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant), RectangleShape),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                    style     = MaterialTheme.typography.titleMedium,
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment     = Alignment.Top
                ) {
                    // Edit Buyer spinner
                    Column(Modifier.weight(1f)) {
                        Text("Edit Buyer", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        ExposedDropdownMenuBox(
                            expanded         = buyerExpanded,
                            onExpandedChange = { buyerExpanded = !buyerExpanded }
                        ) {
                            OutlinedTextField(
                                value        = buyers.firstOrNull { it.first == selectedBuyerId }?.second
                                    ?: if (buyers.isEmpty()) "Loading buyers…" else "Select Buyer",
                                onValueChange = {},
                                readOnly     = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(buyerExpanded) },
                                modifier     = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded         = buyerExpanded,
                                onDismissRequest = { buyerExpanded = false }
                            ) {
                                buyers.forEach { (id, name) ->
                                    DropdownMenuItem(
                                        text    = { Text(name) },
                                        onClick = {
                                            selectedBuyerId = id
                                            buyerExpanded   = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Add Buyer button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Add Buyer", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick        = { showAddBuyer = true },
                            modifier       = Modifier.size(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }

                // Selected Buyer + Edit link
                selectedBuyerId?.let { id ->
                    val name = buyers.firstOrNull { it.first == id }?.second.orEmpty()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                        TextButton(onClick = { showEditBuyer = true }) {
                            Text("Edit")
                        }
                    }
                }
            }
        }

        // ── Manage Categories Card ───────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant), RectangleShape),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Manage Categories",
                    style     = MaterialTheme.typography.titleMedium,
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Category list
                    Column(
                        Modifier
                            .weight(1f)
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        categories.forEach { cat ->
                            Text(
                                text     = cat,
                                modifier = Modifier.padding(vertical = 4.dp),
                                style    = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Add / Delete buttons
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Add Category", style = MaterialTheme.typography.labelLarge)
                        Button(
                            onClick        = { showAddCategory = true },
                            modifier       = Modifier.size(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+", style = MaterialTheme.typography.titleLarge)
                        }

                        Spacer(Modifier.height(16.dp))

                        Text("Delete Category", style = MaterialTheme.typography.labelLarge)
                        Button(
                            onClick        = { showDeleteCategory = true },
                            modifier       = Modifier.size(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("-", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }

        // Optional date‐picker stub
        if (showDatePicker) {
            DatePickerDialog(
                initialDate     = pickedDate ?: LocalDate.now(),
                onDateSelected  = { pickedDate = it; showDatePicker = false },
                onDismissRequest= { showDatePicker = false }
            )
        }
    }

    // ── Dialog invocations ────────────────────────────────
    if (showAddBuyer) {
        AddBuyerDialog(
            onDismiss = { showAddBuyer = false },
            onSubmit  = { buyer ->
                buyersRef.push().setValue(buyer)
                showAddBuyer = false
            }
        )
    }
    if (showEditBuyer && selectedBuyerId != null) {
        EditBuyerDialog(
            onDismiss = { showEditBuyer = false },
            onSubmit  = { updates ->
                val filtered = updates
                    .filterValues { v -> v is String && (v).isNotBlank() }
                    .mapValues    { it.value}
                if (filtered.isNotEmpty()) {
                    buyersRef.child(selectedBuyerId!!).updateChildren(filtered)
                }
                showEditBuyer = false
            }
        )
    }
    if (showAddCategory) {
        AddCategoryDialog(
            onDismiss = { showAddCategory = false },
            onSubmit  = { name ->
                categoriesRef.child(name).setValue(true)
                showAddCategory = false
            }
        )
    }
    if (showDeleteCategory) {
        DeleteCategoryDialog(
            categories = categories,
            onDismiss  = { showDeleteCategory = false },
            onSubmit   = { name ->
                categoriesRef.child(name).removeValue()
                showDeleteCategory = false
            }
        )
    }
}

// Public stub so the call resolves
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
            TextButton(onClick = { onDateSelected(initialDate) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        }
    )
}
