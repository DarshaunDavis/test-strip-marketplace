package com.lislal.teststripmarketplace.ui.admin

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.database.*
import com.lislal.teststripmarketplace.R
import com.lislal.teststripmarketplace.data.Product

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProductsTab() {
    val dbRef = FirebaseDatabase.getInstance().getReference("barcodes")
    val products = remember { mutableStateListOf<Product>() }

    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var overrideInput by remember { mutableStateOf("") }

    // Placeholder dates (normally would be calculated)
    val dates = listOf("04/25", "05/25", "06/25", "07/25", "08/25", "09/25", "10/25", "11/25", "12/25", "01/26")

    // Load products
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
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Manage Products", style = MaterialTheme.typography.titleLarge)
            Button(onClick = { /* TODO: Add product logic */ }) {
                Text("Add")
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(products) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedProduct = product }
                        )
                        IconButton(onClick = {
                            FirebaseDatabase.getInstance()
                                .getReference("barcodes/${product.category}/${product.barcode}")
                                .removeValue()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }

    selectedProduct?.let { prod ->
        AlertDialog(
            onDismissRequest = { selectedProduct = null },
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(prod.description, style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(R.drawable.fmsalightlogo),
                        contentDescription = "Product Image",
                        modifier = Modifier
                            .height(80.dp)
                            .fillMaxWidth()
                            .clickable { /* TODO: Upload new image */ }
                    )
                    Text(
                        "Category: ${prod.category}",
                        modifier = Modifier.clickable { /* TODO: Edit category */ },
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        "Buyer: Default",
                        modifier = Modifier.clickable { /* TODO: Edit buyer */ },
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        dates.take(5).forEach { date ->
                            Text(date, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        prod.prices.take(5).forEachIndexed { index, price ->
                            Text(
                                text = "$$price",
                                modifier = Modifier.clickable {
                                    editingIndex = index
                                    overrideInput = price.toString()
                                }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        dates.drop(5).forEach { date ->
                            Text(date, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        prod.prices.drop(5).forEachIndexed { i, price ->
                            val index = i + 5
                            Text(
                                text = "$$price",
                                modifier = Modifier.clickable {
                                    editingIndex = index
                                    overrideInput = price.toString()
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedProduct = null }) {
                    Text("OK")
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
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingIndex = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
