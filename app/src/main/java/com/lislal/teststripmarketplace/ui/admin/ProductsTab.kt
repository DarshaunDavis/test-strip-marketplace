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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.database.FirebaseDatabase
import com.lislal.teststripmarketplace.R
import com.lislal.teststripmarketplace.data.Product
import java.text.SimpleDateFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProductsTab() {
    // 1) Firebase refs
    val productsRef    = remember { FirebaseDatabase.getInstance().getReference("products") }
    val lastUpdatedRef = remember { FirebaseDatabase.getInstance().getReference("last updated") }

    // 2) State holders
    val products        = remember { mutableStateListOf<Product>() }
    val categoryLastMap = remember { mutableStateMapOf<String, String>() }
    val buyerMap        = remember { mutableStateMapOf<String, String>() }

    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var editingIndex    by remember { mutableStateOf<Int?>(null) }
    var overrideInput   by remember { mutableStateOf("") }

    // 3) Load “last updated” once
    LaunchedEffect(lastUpdatedRef) {
        lastUpdatedRef.get().addOnSuccessListener { snap ->
            snap.children.forEach { catSnap ->
                catSnap.key?.trim()?.let { cat ->
                    catSnap.getValue(String::class.java)?.trim()?.let { date ->
                        categoryLastMap[cat] = date
                    }
                }
            }
        }
    }

    // 4) Load products once (and remember which buyer key each one uses)
    LaunchedEffect(productsRef) {
        productsRef.get().addOnSuccessListener { snap ->
            products.clear()
            buyerMap.clear()
            snap.children.forEach { prodSnap ->
                val barcode     = prodSnap.key ?: return@forEach
                val category    = prodSnap.child("category")
                    .getValue(String::class.java).orEmpty().trim()
                val description = prodSnap.child("description")
                    .getValue(String::class.java).orEmpty()
                val pricesNode  = prodSnap.child("prices")

                // pick the first buyer sub‐node (e.g. "Strip Flip") or default
                val buyerKey = pricesNode.children.firstOrNull()?.key.orEmpty()
                buyerMap[barcode] = buyerKey

                // parse price1…price10 under that buyer
                val prices = (1..10).map { i ->
                    pricesNode
                        .child(buyerKey)
                        .child("price$i")
                        .value
                        ?.toString()
                        ?.toIntOrNull()
                        ?: 0
                }

                products += Product(barcode, category, description, prices)
            }
        }
    }

    // 5) Main UI
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("Manage Products", style = MaterialTheme.typography.titleLarge)
            Button(onClick = { /* TODO: add product */ }) {
                Text("Add")
            }
        }
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(products) { product ->
                Card(
                    Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            product.description,
                            style    = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedProduct = product }
                        )
                        IconButton(onClick = {
                            productsRef.child(product.barcode).removeValue()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }

    // 6) Price + Date Dialog
    selectedProduct?.let { prod ->
        val buyerKey   = buyerMap[prod.barcode].orEmpty()
        val rawDate    = categoryLastMap[prod.category]
        val dateLabels = getDateLabels(rawDate, prod.prices.size)

        AlertDialog(
            onDismissRequest = { selectedProduct = null },
            title = {
                Text(
                    prod.description,
                    style     = MaterialTheme.typography.titleMedium,
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(
                        painter            = painterResource(id = R.drawable.fmsalightlogo),
                        contentDescription = "Product Image",
                        modifier           = Modifier
                            .size(100.dp)
                            .align(Alignment.CenterHorizontally),
                        contentScale       = ContentScale.Crop
                    )
                    Text(
                        "Category: ${prod.category}",
                        modifier  = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Buyer: $buyerKey",
                        modifier  = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    // top 5 months
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        dateLabels.take(5).forEach { Text(it) }
                    }
                    // top 5 prices
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        prod.prices.take(5).forEachIndexed { i, price ->
                            Text(
                                "$$price",
                                Modifier.clickable {
                                    editingIndex  = i
                                    overrideInput = price.toString()
                                }
                            )
                        }
                    }
                    // bottom 5 months
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        dateLabels.drop(5).forEach { Text(it) }
                    }
                    // bottom 5 prices
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        prod.prices.drop(5).forEachIndexed { i, price ->
                            Text(
                                "$$price",
                                Modifier.clickable {
                                    editingIndex  = i + 5
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

    // 7) Override Price Dialog
    editingIndex?.let { idx ->
        AlertDialog(
            onDismissRequest = { editingIndex = null },
            title = { Text("Override Price") },
            text  = {
                Column {
                    Text("Enter new price for month #${idx + 1}")
                    OutlinedTextField(
                        value           = overrideInput,
                        onValueChange   = { overrideInput = it.filter(Char::isDigit) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine      = true,
                        modifier        = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    overrideInput.toIntOrNull()?.let { newVal ->
                        val buyerKey = buyerMap[selectedProduct!!.barcode].orEmpty()
                        val path     = "products/${selectedProduct!!.barcode}/prices/$buyerKey/price${idx + 1}"
                        FirebaseDatabase.getInstance()
                            .getReference(path)
                            .setValue(newVal)
                        // update local state
                        selectedProduct!!.prices =
                            selectedProduct!!.prices.toMutableList().apply { set(idx, newVal) }
                    }
                    editingIndex = null
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

/**
 * Exactly your Flipping‐app logic:
 * parses M/d/yyyy → builds 10 labels: baseDate+11mo … baseDate+2mo (MM/yy)
 */
fun getDateLabels(lastUpdated: String?, size: Int): List<String> {
    val inputFmt  = SimpleDateFormat("M/d/yyyy", Locale.US)
    val outputFmt = SimpleDateFormat("MM/yy",   Locale.US)

    val baseDate: Date = try {
        inputFmt.parse(lastUpdated ?: "") ?: return List(size) { "N/A" }
    } catch (_: Exception) {
        return List(size) { "N/A" }
    }

    return List(size) { i ->
        Calendar.getInstance().apply {
            time = baseDate
            // i=0 → +11 months; i=9 → +2 months
            add(Calendar.MONTH, (size + 1) - i)
        }.let { outputFmt.format(it.time) }
    }
}
