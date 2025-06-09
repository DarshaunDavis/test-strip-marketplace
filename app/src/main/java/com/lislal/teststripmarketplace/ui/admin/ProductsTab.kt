package com.lislal.teststripmarketplace.ui.admin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.lislal.teststripmarketplace.data.Product
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectWholesalerDialog(
    wholesalers: List<String>,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Wholesaler to Add") },
        text = {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = selected,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Wholesaler") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Select Wholesaler") },
                        onClick = {},
                        enabled = false
                    )
                    wholesalers.sorted().forEach { w ->
                        DropdownMenuItem(
                            text = { Text(w) },
                            onClick = {
                                selected = w
                                expanded = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (selected.isNotBlank()) onSubmit(selected)
                onDismiss()
            }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProductsTab() {
    // Firebase refs
    val productsRef    = remember { FirebaseDatabase.getInstance().getReference("products") }
    val categoriesRef  = remember { FirebaseDatabase.getInstance().getReference("categories") }
    val lastUpdatedRef = remember { FirebaseDatabase.getInstance().getReference("last updated") }
    val wholesalersRef = remember { FirebaseDatabase.getInstance().getReference("wholesalers") }

    // State holders
    val products             = remember { mutableStateListOf<Product>() }
    val categoryLastMap      = remember { mutableStateMapOf<String, String>() }
    val wholesalerMap        = remember { mutableStateMapOf<String, String>() }
    val wholesalerListMap    = remember { mutableStateMapOf<String, List<String>>() }
    val allWholesalers       = remember { mutableStateListOf<String>() }
    val categories           = remember { mutableStateListOf<String>() }

    var selectedProduct         by remember { mutableStateOf<Product?>(null) }
    var editingIndex            by remember { mutableStateOf<Int?>(null) }
    var overrideInput           by remember { mutableStateOf("") }
    var showAddDialog           by remember { mutableStateOf(false) }
    var showScanner             by remember { mutableStateOf(false) }
    var showAddWholesalerDialog by remember { mutableStateOf(false) }
    var productToDelete         by remember { mutableStateOf<Product?>(null) }

    // New-product form state
    var newBarcode       by remember { mutableStateOf("") }
    var expandedCategory by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }

    // Camera permission launcher
    val context = LocalContext.current
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        showScanner = granted.also {
            if (!it) Toast.makeText(context, "Camera permission required to scan barcodes", Toast.LENGTH_SHORT).show()
        }
    }

    // Load last-updated
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

    // Refresh products helper
    fun refreshProducts() {
        productsRef.get().addOnSuccessListener { snap ->
            products.clear(); wholesalerMap.clear(); wholesalerListMap.clear()
            snap.children.forEach { prodSnap ->
                val barcode     = prodSnap.key ?: return@forEach
                val category    = prodSnap.child("category").getValue(String::class.java).orEmpty().trim()
                val description = prodSnap.child("description").getValue(String::class.java).orEmpty()
                val imageUrl    = prodSnap.child("imageUrl").getValue(String::class.java)
                val pricesNode  = prodSnap.child("prices")
                val wholesalers = pricesNode.children.mapNotNull { it.key }
                wholesalerListMap[barcode] = wholesalers
                val defaultWholesaler = wholesalers.firstOrNull().orEmpty()
                wholesalerMap[barcode] = defaultWholesaler
                val prices = (1..10).map { i ->
                    pricesNode.child(defaultWholesaler).child("price$i")
                        .value?.toString()?.toIntOrNull() ?: 0
                }
                products += Product(barcode, category, description, prices, imageUrl)
            }
        }
    }
    LaunchedEffect(productsRef) { refreshProducts() }

    // Load all wholesalers
    LaunchedEffect(wholesalersRef) {
        wholesalersRef.get().addOnSuccessListener { snap ->
            allWholesalers.clear()
            snap.children.mapNotNull { it.child("name").getValue(String::class.java) }
                .forEach { allWholesalers += it }
        }
    }

    // Load categories
    LaunchedEffect(categoriesRef) {
        categoriesRef.get().addOnSuccessListener { snap ->
            categories.clear()
            snap.children.mapNotNull { it.key }.forEach { categories += it }
        }
    }

    // ── Main UI ──────────────────────────────────────────
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Manage Products", style = MaterialTheme.typography.titleLarge)
            Button(onClick = {
                newBarcode = ""; selectedCategory = ""; descriptionInput = ""
                showAddDialog = true
            }) { Text("➕ Add Product") }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(products) { product ->
                Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(product.description, style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f).clickable { selectedProduct = product }
                        )
                        IconButton(onClick = { productToDelete = product }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }

    // ── Delete Confirmation Dialog ───────────────────────
    productToDelete?.let { prod ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title            = { Text("Delete Product") },
            text             = { Text("Are you sure you want to delete \"${prod.description}\"?") },
            confirmButton    = {
                TextButton(onClick = {
                    productsRef.child(prod.barcode).removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Product deleted", Toast.LENGTH_SHORT).show()
                            refreshProducts()
                            productToDelete = null
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to delete product", Toast.LENGTH_SHORT).show()
                            productToDelete = null
                        }
                }) { Text("Delete") }
            },
            dismissButton    = {
                TextButton(onClick = { productToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // ── Add Product Dialog ───────────────────────────────
    if (showAddDialog) {
        val lifecycleOwner = LocalLifecycleOwner.current
        var barcodeView: DecoratedBarcodeView? by remember { mutableStateOf(null) }

        DisposableEffect(lifecycleOwner) {
            val observer = object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) { barcodeView?.resume() }
                override fun onPause(owner: LifecycleOwner) { barcodeView?.pause() }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        val scanCallback = remember {
            object : BarcodeCallback {
                override fun barcodeResult(result: BarcodeResult?) {
                    result?.text?.takeIf { it.isNotBlank() }?.let { code ->
                        productsRef.child(code).get().addOnSuccessListener { snap ->
                            if (snap.exists()) {
                                Toast.makeText(context, "This barcode already exists—cannot duplicate.", Toast.LENGTH_SHORT).show()
                            } else {
                                newBarcode = code
                            }
                            showScanner = false
                        }
                    }
                }
                override fun possibleResultPoints(points: MutableList<ResultPoint>?) = Unit
            }
        }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 8.dp) {
                Column(Modifier.padding(16.dp)) {
                    if (showScanner) {
                        Box(Modifier.fillMaxWidth().height(200.dp)) {
                            AndroidView(factory = { ctx ->
                                DecoratedBarcodeView(ctx).apply {
                                    initializeFromIntent(Intent())
                                    decodeContinuous(scanCallback)
                                    resume()
                                    barcodeView = this
                                }
                            }, modifier = Modifier.fillMaxSize())
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = newBarcode, onValueChange = { newBarcode = it },
                        label = { Text("Barcode") },
                        trailingIcon = {
                            IconButton(onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    showScanner = true
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(expanded = expandedCategory, onExpandedChange = { expandedCategory = !expandedCategory }, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCategory, onValueChange = {},
                            readOnly = true, label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedCategory) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = expandedCategory, onDismissRequest = { expandedCategory = false }) {
                            DropdownMenuItem(text = { Text("Select Category") }, onClick = {}, enabled = false)
                            categories.sorted().forEach { cat ->
                                DropdownMenuItem(text = { Text(cat) }, onClick = {
                                    selectedCategory = cat
                                    expandedCategory = false
                                })
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = descriptionInput,
                        onValueChange = { descriptionInput = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            when {
                                newBarcode.isBlank() -> Toast.makeText(context, "Barcode cannot be blank.", Toast.LENGTH_SHORT).show()
                                selectedCategory.isBlank() -> Toast.makeText(context, "Please select a category.", Toast.LENGTH_SHORT).show()
                                descriptionInput.isBlank() -> Toast.makeText(context, "Description cannot be blank.", Toast.LENGTH_SHORT).show()
                                else -> {
                                    val data = mapOf("category" to selectedCategory, "description" to descriptionInput)
                                    productsRef.child(newBarcode).setValue(data)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Product added successfully.", Toast.LENGTH_SHORT).show()
                                            showAddDialog = false
                                            refreshProducts()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "Failed to add product.", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                        }) { Text("Save") }
                    }
                }
            }
        }
    }

    // ── PriceDialog (unchanged) ───────────────────────────
    selectedProduct?.let { prod ->
        PriceDialog(
            product              = prod,
            wholesalerKey        = wholesalerMap[prod.barcode].orEmpty(),
            wholesalers          = wholesalerListMap[prod.barcode] ?: emptyList(),
            dateLabels           = getDateLabels(categoryLastMap[prod.category], prod.prices.size),
            prices               = prod.prices,
            onPriceClick         = { idx ->
                editingIndex  = idx
                overrideInput = prod.prices[idx].toString()
            },
            onImageClick         = { uri -> uploadImageForProduct(prod.barcode, uri) },
            onWholesalerSelected = { newWholesaler ->
                wholesalerMap[prod.barcode] = newWholesaler
                productsRef.child(prod.barcode).child("prices").child(newWholesaler).get()
                    .addOnSuccessListener { snap ->
                        val fresh = (1..10).map { i ->
                            snap.child("price$i").value?.toString()?.toIntOrNull() ?: 0
                        }
                        val idx = products.indexOfFirst { it.barcode == prod.barcode }
                        if (idx >= 0) {
                            products[idx] = prod.copy(prices = fresh)
                            selectedProduct = products[idx]
                        }
                    }
            },
            onAddWholesaler = { showAddWholesalerDialog = true },
            onDismiss        = { selectedProduct = null }
        )
    }

    // ── Select Wholesaler Dialog ─────────────────────────
    if (showAddWholesalerDialog && selectedProduct != null) {
        SelectWholesalerDialog(
            wholesalers = allWholesalers,
            onDismiss   = { showAddWholesalerDialog = false },
            onSubmit    = { newWholesaler ->
                val code = selectedProduct!!.barcode
                wholesalerListMap[code] = wholesalerListMap[code].orEmpty() + listOf(newWholesaler)
                wholesalerMap[code] = newWholesaler
                val init = (1..10).associate { i -> "price$i" to 0 }
                productsRef.child(code).child("prices").child(newWholesaler).setValue(init)
                products.indexOfFirst { it.barcode == code }.takeIf { it >= 0 }?.also { idx ->
                    products[idx] = products[idx].copy(prices = List(10) { 0 })
                    selectedProduct = products[idx]
                }
                showAddWholesalerDialog = false
            }
        )
    }

    // ── Override Price Dialog ────────────────────────────
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
            confirmButton    = {
                TextButton(onClick = {
                    overrideInput.toIntOrNull()?.let { newVal ->
                        val code = selectedProduct!!.barcode
                        val wKey = wholesalerMap[code].orEmpty()
                        productsRef.child(code)
                            .child("prices").child(wKey)
                            .child("price${idx + 1}")
                            .setValue(newVal)
                            .addOnSuccessListener {
                                // update local state
                                val prodIdx = products.indexOfFirst { it.barcode == code }
                                if (prodIdx >= 0) {
                                    val updated = products[prodIdx].copy(
                                        prices = products[prodIdx].prices.toMutableList().apply { set(idx, newVal) }
                                    )
                                    products[prodIdx] = updated
                                    selectedProduct = updated
                                }
                            }
                    }
                    editingIndex = null
                }) { Text("Save") }
            },
            dismissButton    = {
                TextButton(onClick = { editingIndex = null }) { Text("Cancel") }
            }
        )
    }
}

fun uploadImageForProduct(barcode: String, uri: Uri) {
    val storageRef = FirebaseStorage.getInstance().getReference("product_images/$barcode.jpg")
    storageRef.putFile(uri)
        .continueWithTask { task ->
            if (!task.isSuccessful) throw task.exception!!
            storageRef.downloadUrl
        }
        .addOnSuccessListener { url ->
            FirebaseDatabase.getInstance()
                .getReference("products")
                .child(barcode)
                .child("imageUrl")
                .setValue(url.toString())
        }
}

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
