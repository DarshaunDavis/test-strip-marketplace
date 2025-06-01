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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
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
fun SelectBuyerDialog(
    buyers: List<String>,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Buyer to Add") },
        text = {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = selected,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Buyer") },
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
                        text = { Text("Select Buyer") },
                        onClick = {},
                        enabled = false
                    )
                    buyers.sorted().forEach { b ->
                        DropdownMenuItem(
                            text = { Text(b) },
                            onClick = {
                                selected = b
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
    val buyersRef      = remember { FirebaseDatabase.getInstance().getReference("buyers") }

    // State holders
    val products        = remember { mutableStateListOf<Product>() }
    val categoryLastMap = remember { mutableStateMapOf<String, String>() }
    val buyerMap        = remember { mutableStateMapOf<String, String>() }
    val buyerListMap    = remember { mutableStateMapOf<String, List<String>>() }
    val allBuyers       = remember { mutableStateListOf<String>() }
    val categories      = remember { mutableStateListOf<String>() }

    var selectedProduct    by remember { mutableStateOf<Product?>(null) }
    var editingIndex       by remember { mutableStateOf<Int?>(null) }
    var overrideInput      by remember { mutableStateOf("") }
    var showAddDialog      by remember { mutableStateOf(false) }
    var showScanner        by remember { mutableStateOf(false) }
    var showAddBuyerDialog by remember { mutableStateOf(false) }
    var productToDelete    by remember { mutableStateOf<Product?>(null) }

    // New-product form state
    var newBarcode        by remember { mutableStateOf("") }
    var expandedCategory  by remember { mutableStateOf(false) }
    var selectedCategory  by remember { mutableStateOf("") }
    var descriptionInput  by remember { mutableStateOf("") }

    // Request camera permission launcher
    val context = LocalContext.current
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showScanner = true
        } else {
            Toast.makeText(context, "Camera permission required to scan barcodes", Toast.LENGTH_SHORT).show()
            showScanner = false
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

    // Load products (and refresh when new ones are added)
    fun refreshProducts() {
        productsRef.get().addOnSuccessListener { snap ->
            products.clear()
            buyerMap.clear()
            buyerListMap.clear()
            snap.children.forEach { prodSnap ->
                val barcode     = prodSnap.key ?: return@forEach
                val category    = prodSnap.child("category").getValue(String::class.java).orEmpty().trim()
                val description = prodSnap.child("description").getValue(String::class.java).orEmpty()
                val imageUrl    = prodSnap.child("imageUrl").getValue(String::class.java)
                val pricesNode  = prodSnap.child("prices")
                val buyers      = pricesNode.children.mapNotNull { it.key }
                buyerListMap[barcode] = buyers
                val defaultBuyer = buyers.firstOrNull().orEmpty()
                buyerMap[barcode] = defaultBuyer
                val prices = (1..10).map { i ->
                    pricesNode.child(defaultBuyer).child("price$i").value
                        ?.toString()?.toIntOrNull() ?: 0
                }
                products += Product(barcode, category, description, prices, imageUrl)
            }
        }
    }
    LaunchedEffect(productsRef) { refreshProducts() }

    // Load all buyers
    LaunchedEffect(buyersRef) {
        buyersRef.get().addOnSuccessListener { snap ->
            allBuyers.clear()
            snap.children.mapNotNull { it.child("name").getValue(String::class.java) }
                .forEach { allBuyers += it }
        }
    }

    // Load categories (use keys since values may be Boolean)
    LaunchedEffect(categoriesRef) {
        categoriesRef.get().addOnSuccessListener { snap ->
            categories.clear()
            snap.children.mapNotNull { it.key }
                .forEach { categories += it }
        }
    }

    // Main UI
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("Manage Products", style = MaterialTheme.typography.titleLarge)
            Button(onClick = {
                // Reset form state when opening
                newBarcode = ""
                selectedCategory = ""
                descriptionInput = ""
                showAddDialog = true
            }) { Text("➕ Add Product") }
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
                            productToDelete = product
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    productToDelete?.let { prod ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Delete Product") },
            text = { Text("Are you sure you want to delete \"${prod.description}\"?") },
            confirmButton = {
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
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Add Product Dialog
    if (showAddDialog) {
        val lifecycleOwner = LocalLifecycleOwner.current
        var barcodeView: DecoratedBarcodeView? by remember { mutableStateOf(null) }

        // Lifecycle handling for scanner
        DisposableEffect(lifecycleOwner) {
            val observer = object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) { barcodeView?.resume() }
                override fun onPause(owner: LifecycleOwner) { barcodeView?.pause() }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        // Scanner callback
        val scanCallback = remember {
            object : BarcodeCallback {
                override fun barcodeResult(result: BarcodeResult?) {
                    result?.text?.takeIf { it.isNotBlank() }?.let { code ->
                        // Check for duplicate
                        productsRef.child(code).get().addOnSuccessListener { snap ->
                            if (snap.exists()) {
                                Toast.makeText(
                                    context,
                                    "This barcode already exists—cannot duplicate.",
                                    Toast.LENGTH_SHORT
                                ).show()
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
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 8.dp
            ) {
                Column(Modifier.padding(16.dp)) {
                    // Embedded Scanner View
                    if (showScanner) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    DecoratedBarcodeView(ctx).apply {
                                        initializeFromIntent(Intent())
                                        decodeContinuous(scanCallback)
                                        resume()
                                        barcodeView = this
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    // Barcode Field with Scan Icon
                    OutlinedTextField(
                        value = newBarcode,
                        onValueChange = { newBarcode = it },
                        label = { Text("Barcode") },
                        trailingIcon = {
                            IconButton(onClick = {
                                // Check camera permission before showing scanner
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    showScanner = true
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan Barcode"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(Modifier.height(8.dp))
                    // Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedCategory,
                        onExpandedChange = { expandedCategory = !expandedCategory },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expandedCategory)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCategory,
                            onDismissRequest = { expandedCategory = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Select Category") },
                                onClick = {},
                                enabled = false
                            )
                            categories.sorted().forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        expandedCategory = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Description Field
                    OutlinedTextField(
                        value = descriptionInput,
                        onValueChange = { descriptionInput = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false
                    )
                    Spacer(Modifier.height(16.dp))
                    // Buttons Row
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            // Validation
                            when {
                                newBarcode.isBlank() -> {
                                    Toast.makeText(context, "Barcode cannot be blank.", Toast.LENGTH_SHORT).show()
                                }
                                selectedCategory.isBlank() -> {
                                    Toast.makeText(context, "Please select a category.", Toast.LENGTH_SHORT).show()
                                }
                                descriptionInput.isBlank() -> {
                                    Toast.makeText(context, "Description cannot be blank.", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    // Write to Firebase
                                    val data = mapOf(
                                        "category"    to selectedCategory,
                                        "description" to descriptionInput
                                    )
                                    productsRef.child(newBarcode).setValue(data)
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                context,
                                                "Product added successfully.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            showAddDialog = false
                                            refreshProducts()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(
                                                context,
                                                "Failed to add product.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                            }
                        }) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    // PriceDialog (unchanged)...
    selectedProduct?.let { prod ->
        PriceDialog(
            product         = prod,
            buyerKey        = buyerMap[prod.barcode].orEmpty(),
            buyers          = buyerListMap[prod.barcode] ?: emptyList(),
            dateLabels      = getDateLabels(categoryLastMap[prod.category], prod.prices.size),
            prices          = prod.prices,
            onPriceClick    = { idx ->
                editingIndex = idx
                overrideInput = prod.prices[idx].toString()
            },
            onImageClick    = { uri ->
                uploadImageForProduct(prod.barcode, uri)
            },
            onBuyerSelected = { newBuyer ->
                buyerMap[prod.barcode] = newBuyer
                productsRef.child(prod.barcode)
                    .child("prices").child(newBuyer).get()
                    .addOnSuccessListener { snap ->
                        val fresh = (1..10).map { i ->
                            snap.child("price$i").value?.toString()?.toIntOrNull() ?: 0
                        }
                        products.indexOfFirst { it.barcode == prod.barcode }
                            .takeIf { it >= 0 }?.let { idx ->
                                val updated    = prod.copy(prices = fresh)
                                products[idx] = updated
                                selectedProduct = updated
                            }
                    }
            },
            onAddBuyer      = { showAddBuyerDialog = true },
            onDismiss       = { selectedProduct = null }
        )
    }

    // Select Buyer Dialog
    if (showAddBuyerDialog && selectedProduct != null) {
        SelectBuyerDialog(
            buyers    = allBuyers,
            onDismiss = { showAddBuyerDialog = false },
            onSubmit  = { newBuyer ->
                val code = selectedProduct!!.barcode
                buyerListMap[code] = buyerListMap[code].orEmpty() + listOf(newBuyer)
                buyerMap[code] = newBuyer
                val init = (1..10).associate { i -> "price$i" to 0 }
                productsRef.child(code).child("prices").child(newBuyer).setValue(init)
                selectedProduct = selectedProduct!!.copy(prices = List(10) { 0 })
                showAddBuyerDialog = false
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
            FirebaseDatabase.getInstance().getReference("products")
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
