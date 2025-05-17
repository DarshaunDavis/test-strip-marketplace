package com.lislal.teststripmarketplace.ui.admin

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.database.*

data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val buyers: List<String>,
    val images: List<String>
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProductsTab() {
    // ── Firebase refs ────────────────────────────────────
    val productsRef = remember { FirebaseDatabase.getInstance().getReference("products") }
    val buyersRef   = remember { FirebaseDatabase.getInstance().getReference("buyers") }

    // ── In‐memory caches ──────────────────────────────────
    val products = remember { mutableStateListOf<Product>() }
    val buyers   = remember { mutableStateListOf<Pair<String, String>>() }

    // ── Load + listen products ───────────────────────────
    LaunchedEffect(productsRef) {
        productsRef.get().addOnSuccessListener { snap ->
            products.clear()
            snap.children.forEach { child ->
                val id    = child.key ?: return@forEach
                val name  = child.child("name").getValue(String::class.java).orEmpty()
                val price = child.child("price").getValue(Double::class.java) ?: 0.0
                val bList = child.child("buyers")
                    .children.mapNotNull { it.key }
                val iList = child.child("images")
                    .children.mapNotNull { it.getValue(String::class.java) }
                products += Product(id, name, price, bList, iList)
            }
        }
    }
    DisposableEffect(productsRef) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                products.clear()
                snap.children.forEach { child ->
                    val id    = child.key ?: return@forEach
                    val name  = child.child("name").getValue(String::class.java).orEmpty()
                    val price = child.child("price").getValue(Double::class.java) ?: 0.0
                    val bList = child.child("buyers")
                        .children.mapNotNull { it.key }
                    val iList = child.child("images")
                        .children.mapNotNull { it.getValue(String::class.java) }
                    products += Product(id, name, price, bList, iList)
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        productsRef.addValueEventListener(listener)
        onDispose { productsRef.removeEventListener(listener) }
    }

    // ── Load + listen buyers ─────────────────────────────
    LaunchedEffect(buyersRef) {
        buyersRef.get().addOnSuccessListener { snap ->
            buyers.clear()
            snap.children.forEach { child ->
                child.key?.let { id ->
                    val name = child.child("name").getValue(String::class.java).orEmpty()
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
                        val name = child.child("name").getValue(String::class.java).orEmpty()
                        buyers += id to name
                    }
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        buyersRef.addValueEventListener(listener)
        onDispose { buyersRef.removeEventListener(listener) }
    }

    // ── UI state ─────────────────────────────────────────
    var showAddProduct      by remember { mutableStateOf(false) }
    var showDeleteProduct   by remember { mutableStateOf(false) }
    var showEditPriceFor    by remember { mutableStateOf<Product?>(null) }
    var showManageBuyersFor by remember { mutableStateOf<Product?>(null) }
    var showManageImagesFor by remember { mutableStateOf<Product?>(null) }

    // ── Single scrollable container ──────────────────────
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Spacer(Modifier.height(16.dp)) }

        // ── Header + Add/Delete ────────────────────────────
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment    = Alignment.CenterVertically
            ) {
                Text("Manage Products", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showAddProduct = true })    { Text("+ Add")    }
                    Button(onClick = { showDeleteProduct = true }) { Text("- Delete") }
                }
            }
        }

        // ── Product Items (now only show name) ─────────────
        items(products) { product ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation= CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // ONLY THE NAME
                    Text(
                        product.name,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.weight(1f)
                    )

                    // edit price
                    IconButton(onClick = { showEditPriceFor = product }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Price")
                    }
                    // manage buyers
                    IconButton(onClick = { showManageBuyersFor = product }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Manage Buyers")
                    }
                    // manage images
                    IconButton(onClick = { showManageImagesFor = product }) {
                        Icon(Icons.Default.Photo, contentDescription = "Images")
                    }
                }
            }
        }
    }

    // ── DIALOGS ───────────────────────────────────────────

    if (showAddProduct) {
        AddProductDialog(
            onDismiss = { showAddProduct = false },
            onSubmit  = { name, price ->
                productsRef.push().apply {
                    child("name").setValue(name)
                    child("price").setValue(price)
                }
                showAddProduct = false
            }
        )
    }

    if (showDeleteProduct) {
        DeleteProductDialog(
            items     = products.map { it.name },
            onDismiss = { showDeleteProduct = false },
            onSubmit  = { name ->
                products.find { it.name == name }?.let {
                    productsRef.child(it.id).removeValue()
                }
                showDeleteProduct = false
            }
        )
    }

    showEditPriceFor?.let { product ->
        EditPriceDialog(
            currentPrice = product.price,
            onDismiss    = { showEditPriceFor = null },
            onSubmit     = { newPrice ->
                productsRef.child(product.id).child("price").setValue(newPrice)
                showEditPriceFor = null
            }
        )
    }

    showManageBuyersFor?.let { product ->
        ManageProductBuyersDialog(
            allBuyers      = buyers,
            initialMembers = product.buyers,
            onDismiss      = { showManageBuyersFor = null },
            onSubmit       = { sel ->
                productsRef.child(product.id).child("buyers")
                    .setValue(sel.associateWith { true })
                showManageBuyersFor = null
            }
        )
    }

    showManageImagesFor?.let { product ->
        ManageProductImagesDialog(
            initialUrls = product.images,
            onDismiss   = { showManageImagesFor = null },
            onSubmit    = { urls ->
                productsRef.child(product.id).child("images")
                    .setValue(urls)
                showManageImagesFor = null
            }
        )
    }
}

// --- The dialog composables remain unchanged from the version I shared previously ---

//
// ── DIALOG COMPOSABLES ───────────────────────────────────
//

@Composable
fun AddProductDialog(
    onDismiss: ()->Unit,
    onSubmit: (name: String, price: Double)->Unit
) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Add Product") },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                priceText.toDoubleOrNull()?.let { p -> onSubmit(name.trim(), p) }
            }) { Text("Submit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteProductDialog(
    items: List<String>,
    onDismiss: ()->Unit,
    onSubmit: (String)->Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Delete Product") },
        text    = {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selected ?: "Select product",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    items.forEach { itName ->
                        DropdownMenuItem(
                            text = { Text(itName) },
                            onClick = {
                                selected = itName
                                expanded = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { selected?.let(onSubmit) }) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditPriceDialog(
    currentPrice: Double,
    onDismiss: ()->Unit,
    onSubmit: (Double)->Unit
) {
    var text by remember { mutableStateOf(currentPrice.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Edit Price") },
        text    = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("New Price") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(onClick = { text.toDoubleOrNull()?.let(onSubmit) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageProductBuyersDialog(
    allBuyers: List<Pair<String, String>>,
    initialMembers: List<String>,
    onDismiss: ()->Unit,
    onSubmit: (List<String>)->Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>().apply { addAll(initialMembers) } }
    var pick by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Buyers") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = pick
                            ?.let { id -> allBuyers.firstOrNull { it.first == id }?.second }
                            ?: "Add buyer",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        allBuyers.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    pick = id
                                    expanded = false
                                    if (id !in selected) selected.add(id)
                                }
                            )
                        }
                    }
                }

                selected.forEach { id ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            allBuyers.firstOrNull { it.first == id }?.second ?: id,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { selected.remove(id) }) {
                            Text("Remove")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(selected.toList()) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ManageProductImagesDialog(
    initialUrls: List<String>,
    onDismiss: ()->Unit,
    onSubmit: (List<String>)->Unit
) {
    var urlText by remember { mutableStateOf("") }
    val urls = remember { mutableStateListOf<String>().apply { addAll(initialUrls) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Images") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("Image URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = {
                    if (urlText.isNotBlank()) {
                        urls.add(urlText.trim())
                        urlText = ""
                    }
                }) {
                    Text("Add URL")
                }
                urls.forEach { u ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(u, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { urls.remove(u) }) {
                            Text("Delete")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(urls.toList()) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
