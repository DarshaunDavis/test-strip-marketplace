package com.lislal.teststripmarketplace.ui.admin

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.lislal.teststripmarketplace.data.Product
import java.text.SimpleDateFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProductsTab() {
    // 1) Firebase refs
    val productsRef    = remember { FirebaseDatabase.getInstance().getReference("products") }
    val lastUpdatedRef = remember { FirebaseDatabase.getInstance().getReference("last updated") }

    // 2) State
    val products        = remember { mutableStateListOf<Product>() }
    val categoryLastMap = remember { mutableStateMapOf<String, String>() }
    val buyerMap        = remember { mutableStateMapOf<String, String>() }
    val buyerListMap    = remember { mutableStateMapOf<String, List<String>>() }

    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var editingIndex    by remember { mutableStateOf<Int?>(null) }
    var overrideInput   by remember { mutableStateOf("") }

    // 3) Load last‐updated dates
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

    // 4) Load all products (+ their buyer‐keys + imageUrl)
    LaunchedEffect(productsRef) {
        productsRef.get().addOnSuccessListener { snap ->
            products.clear()
            buyerMap.clear()
            buyerListMap.clear()

            snap.children.forEach { prodSnap ->
                val barcode     = prodSnap.key ?: return@forEach
                val category    = prodSnap.child("category")
                    .getValue(String::class.java).orEmpty().trim()
                val description = prodSnap.child("description")
                    .getValue(String::class.java).orEmpty()
                val imageUrl    = prodSnap.child("imageUrl")
                    .getValue(String::class.java)

                // gather all buyer‐keys under “prices”
                val pricesNode = prodSnap.child("prices")
                val buyers     = pricesNode.children.mapNotNull { it.key }
                buyerListMap[barcode] = buyers

                // pick a default buyer
                val buyerKey = buyers.firstOrNull().orEmpty()
                buyerMap[barcode] = buyerKey

                // load price1…price10 for that buyer
                val prices = (1..10).map { i ->
                    pricesNode
                        .child(buyerKey)
                        .child("price$i")
                        .value
                        ?.toString()
                        ?.toIntOrNull()
                        ?: 0
                }

                products += Product(
                    barcode     = barcode,
                    category    = category,
                    description = description,
                    prices      = prices,
                    imageUrl    = imageUrl
                )
            }
        }
    }

    // 5) Main list UI
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("Manage Products", style = MaterialTheme.typography.titleLarge)
            Button(onClick = { /* TODO: add product */ }) { Text("Add") }
        }
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(products) { product ->
                Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
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

    // 6) Price + Date + Image + Buyer dialog
    selectedProduct?.let { prod ->
        PriceDialog(
            product         = prod,
            buyerKey        = buyerMap[prod.barcode].orEmpty(),
            buyers          = buyerListMap[prod.barcode] ?: emptyList(),
            dateLabels      = getDateLabels(categoryLastMap[prod.category], prod.prices.size),
            prices          = prod.prices,
            onPriceClick    = { idx ->
                editingIndex  = idx
                overrideInput = prod.prices[idx].toString()
            },
            onImageClick    = { uri ->
                uploadImageForProduct(prod.barcode, uri)
            },
            onBuyerSelected = { newBuyer ->
                // switch current buyer
                buyerMap[prod.barcode] = newBuyer
                // TODO: reload prod.prices for newBuyer
            },
            onAddBuyer      = {
                // TODO: launch “add new buyer” flow
            },
            onDismiss       = { selectedProduct = null }
        )
    }

    // 7) Override‐price dialog
    editingIndex?.let { idx ->
        AlertDialog(
            onDismissRequest = { editingIndex = null },
            title            = { Text("Override Price") },
            text             = {
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
                        selectedProduct!!.prices =
                            selectedProduct!!.prices.toMutableList().apply { set(idx, newVal) }
                    }
                    editingIndex = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingIndex = null }) { Text("Cancel") }
            }
        )
    }
}

// Uploads picked image → Firebase Storage → writes its URL back under products/<barcode>/imageUrl
fun uploadImageForProduct(barcode: String, uri: Uri) {
    val storageRef = FirebaseStorage
        .getInstance()
        .getReference("product_images/$barcode.jpg")

    storageRef.putFile(uri)
        .continueWithTask { task ->
            if (!task.isSuccessful) throw task.exception!!
            storageRef.downloadUrl
        }
        .addOnSuccessListener { downloadUrl ->
            FirebaseDatabase.getInstance()
                .getReference("products")
                .child(barcode)
                .child("imageUrl")
                .setValue(downloadUrl.toString())
        }
}

/** Builds N month-labels (MM/yy) counting +11…+2 months from M/d/yyyy */
fun getDateLabels(lastUpdated: String?, size: Int): List<String> {
    val parser  = SimpleDateFormat("M/d/yyyy", Locale.US)
    val display = SimpleDateFormat("MM/yy",    Locale.US)
    val base    = try {
        parser.parse(lastUpdated ?: "") ?: return List(size) { "N/A" }
    } catch (_: Exception) {
        return List(size) { "N/A" }
    }
    return List(size) { i ->
        Calendar.getInstance().apply {
            time = base
            add(Calendar.MONTH, (size + 1) - i)
        }.let { display.format(it.time) }
    }
}
