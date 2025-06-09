package com.lislal.teststripmarketplace.ui.admin

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lislal.teststripmarketplace.R
import com.lislal.teststripmarketplace.data.Product

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PriceDialog(
    product: Product,
    wholesalerKey: String,
    wholesalers: List<String>,
    dateLabels: List<String>,
    prices: List<Int>,
    onPriceClick: (Int) -> Unit,
    onImageClick: (Uri) -> Unit,
    onWholesalerSelected: (String) -> Unit,
    onAddWholesaler: () -> Unit,
    onDismiss: () -> Unit
) {
    // 1) image‐picker launcher
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> uri?.let(onImageClick) }
    )

    // 2) spinner state
    var expanded      by remember { mutableStateOf(false) }
    var selectedWholesaler by remember { mutableStateOf(wholesalerKey) }

    // sync local spinner selection when parent wholesalerKey changes
    LaunchedEffect(wholesalerKey) {
        selectedWholesaler = wholesalerKey
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text      = product.description,
                style     = MaterialTheme.typography.titleMedium,
                modifier  = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // — AsyncImage with your URL (or fallback logo)
                AsyncImage(
                    model               = product.imageUrl,
                    contentDescription  = "Product Image",
                    placeholder         = painterResource(R.drawable.fmsalightlogo),
                    error               = painterResource(R.drawable.fmsalightlogo),
                    contentScale        = ContentScale.Crop,
                    modifier            = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterHorizontally)
                        .clickable { pickImageLauncher.launch("image/*") }
                )

                // — Category
                Text(
                    "Category: ${product.category}",
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // — Wholesaler spinner
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value         = selectedWholesaler,
                        onValueChange = { },
                        readOnly      = true,
                        label         = { Text("Wholesaler") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier      = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        // non-clickable prompt
                        DropdownMenuItem(
                            text    = { Text("Select wholesaler") },
                            enabled = false,
                            onClick = {}
                        )
                        // existing wholesalers
                        wholesalers.forEach { b ->
                            DropdownMenuItem(
                                text    = { Text(b) },
                                onClick = {
                                    selectedWholesaler = b
                                    expanded      = false
                                    onWholesalerSelected(b)
                                }
                            )
                        }
                        // add‐wholesaler option
                        DropdownMenuItem(
                            text    = { Text("Add wholesaler") },
                            onClick = {
                                expanded = false
                                onAddWholesaler()
                            }
                        )
                    }
                }

                // — top 5 month labels & prices
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    dateLabels.take(5).forEach { Text(it) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    prices.take(5).forEachIndexed { idx, price ->
                        Text(
                            "$$price",
                            modifier = Modifier.clickable { onPriceClick(idx) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // — bottom 5 month labels & prices
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    dateLabels.drop(5).forEach { Text(it) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    prices.drop(5).forEachIndexed { dropIdx, price ->
                        val idx = dropIdx + 5
                        Text(
                            "$$price",
                            modifier = Modifier.clickable { onPriceClick(idx) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
