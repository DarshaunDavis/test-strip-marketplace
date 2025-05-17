package com.lislal.teststripmarketplace.ui.admin

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.database.*
import com.lislal.teststripmarketplace.data.Product

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProductsTab() {
    val dbRef = FirebaseDatabase.getInstance().getReference("barcodes")
    val products = remember { mutableStateListOf<Product>() }

    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var overrideInput by remember { mutableStateOf("") }

    // Load all barcodes
    LaunchedEffect(Unit) {
        dbRef.get().addOnSuccessListener { snapshot ->
            products.clear()
            snapshot.children.forEach { categorySnap ->
                val category = categorySnap.key ?: return@forEach
                categorySnap.children.forEach { barcodeSnap ->
                    val barcode = barcodeSnap.key ?: return@forEach
                    val description = barcodeSnap.child("description").getValue(String::class.java) ?: ""
                    val prices = (1..10).map { i ->
                        barcodeSnap.child("Default").child("price$i").getValue(Int::class.java) ?: 0
                    }
                    products += Product(barcode, category, description, prices)
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // ── Header and Add Button ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Manage Products", style = MaterialTheme.typography.titleLarge)
            Button(onClick = {
                // TODO: Add product logic will be added later
            }) {
                Text("Add")
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(products) { product ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedProduct = product },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(text = product.description, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    selectedProduct?.let { prod ->
        AlertDialog(
            onDismissRequest = { selectedProduct = null },
            title = { Text(prod.description) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    prod.prices.forEachIndexed { index, price ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editingIndex = index
                                    overrideInput = price.toString()
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Month ${index + 1}: $${price}", Modifier.padding(8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedProduct = null }) {
                    Text("Close")
                }
            }
        )
    }

    editingIndex?.let { idx ->
        AlertDialog(
            onDismissRequest = { editingIndex = null },
            title = { Text("Override Price") },
            text = {
                Column {
                    Text("Enter new price for month #${idx + 1}")
                    OutlinedTextField(
                        value = overrideInput,
                        onValueChange = { overrideInput = it.filter(Char::isDigit) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    overrideInput.toIntOrNull()?.let { newVal ->
                        val path = "barcodes/${selectedProduct!!.category}/${selectedProduct!!.barcode}/Default/price${idx + 1}"
                        FirebaseDatabase.getInstance().getReference(path).setValue(newVal)
                        selectedProduct!!.prices = selectedProduct!!.prices.toMutableList().apply { set(idx, newVal) }
                        editingIndex = null
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingIndex = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
