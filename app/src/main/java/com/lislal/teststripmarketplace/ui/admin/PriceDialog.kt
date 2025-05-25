package com.lislal.teststripmarketplace.ui.admin

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lislal.teststripmarketplace.R
import com.lislal.teststripmarketplace.data.Product

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PriceDialog(
    product: Product,
    buyerKey: String,
    dateLabels: List<String>,
    prices: List<Int>,
    onPriceClick: (Int) -> Unit,
    onImageClick: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    // launcher to pick a new image from device
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> uri?.let(onImageClick) }
    )

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

                Text(
                    "Category: ${product.category}",
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Buyer: $buyerKey",
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // top 5 month labels
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
                    prices.take(5).forEachIndexed { idx, price ->
                        Text(
                            text = "$$price",
                            modifier = Modifier.clickable { onPriceClick(idx) }
                        )
                    }
                }
                // bottom 5 month labels
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
                    prices.drop(5).forEachIndexed { dropIdx, price ->
                        val idx = dropIdx + 5
                        Text(
                            text = "$$price",
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
