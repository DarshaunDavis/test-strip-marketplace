package com.lislal.teststripmarketplace.ui.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.lislal.teststripmarketplace.R
import com.lislal.teststripmarketplace.data.Product

/**
 * Shows a product’s image (clickable), category, buyer, a 2×5 grid of dates & prices,
 * and an OK button to dismiss.
 */
@Composable
fun PriceDialog(
    product: Product,
    buyerKey: String,
    dateLabels: List<String>,
    onImageClick: () -> Unit,
    onPriceClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text      = product.description,
                modifier  = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // placeholder or will show loaded image later
                Image(
                    painter            = painterResource(R.drawable.fmsalightlogo),
                    contentDescription = "Product Image",
                    modifier           = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterHorizontally)
                        .clickable(onClick = onImageClick),
                    contentScale       = ContentScale.Crop
                )

                Text(
                    "Category: ${product.category}",
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    "Buyer: $buyerKey",
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // top row of dates
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    dateLabels.take(5).forEach { Text(it) }
                }
                // top row of prices
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    product.prices.take(5).forEachIndexed { idx, price ->
                        Text(
                            text     = "$$price",
                            modifier = Modifier.clickable { onPriceClick(idx) }
                        )
                    }
                }
                // bottom row of dates
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    dateLabels.drop(5).forEach { Text(it) }
                }
                // bottom row of prices
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    product.prices.drop(5).forEachIndexed { di, price ->
                        Text(
                            text     = "$$price",
                            modifier = Modifier.clickable { onPriceClick(di + 5) }
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
