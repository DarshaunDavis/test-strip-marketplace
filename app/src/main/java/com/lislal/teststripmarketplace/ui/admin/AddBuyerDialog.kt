package com.lislal.teststripmarketplace.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AddBuyerDialog(
    onDismiss: () -> Unit,
    onSubmit: (Buyer) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // FocusRequesters
    val nameReq    = remember { FocusRequester() }
    val addressReq = remember { FocusRequester() }
    val suiteReq   = remember { FocusRequester() }
    val cityReq    = remember { FocusRequester() }
    val stateReq   = remember { FocusRequester() }
    val zipReq     = remember { FocusRequester() }

    // Form state
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var suite by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var zip by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Buyer") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { addressReq.requestFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameReq)
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { suiteReq.requestFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(addressReq)
                )

                OutlinedTextField(
                    value = suite,
                    onValueChange = { suite = it },
                    label = { Text("Suite") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { cityReq.requestFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(suiteReq)
                )

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { stateReq.requestFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(cityReq)
                )

                // State as 2-letter all-caps
                OutlinedTextField(
                    value = state,
                    onValueChange = {
                        val filtered = it.uppercase().filter { ch -> ch.isLetter() }
                        state = if (filtered.length <= 2) filtered else filtered.take(2)
                    },
                    label = { Text("State (2-letter)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { zipReq.requestFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(stateReq)
                )

                OutlinedTextField(
                    value = zip,
                    onValueChange = {
                        if (it.length <= 5 && it.all(Char::isDigit)) zip = it
                    },
                    label = { Text("Zip Code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(zipReq)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (
                    name.isNotBlank() &&
                    address.isNotBlank() &&
                    city.isNotBlank() &&
                    state.length == 2 &&
                    zip.length == 5
                ) {
                    onSubmit(
                        Buyer(
                            name = name.trim(),
                            address = address.trim(),
                            suite = suite.trim(),
                            city = city.trim(),
                            state = state,
                            zip = zip
                        )
                    )
                }
            }) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

data class Buyer(
    val name: String,
    val address: String,
    val suite: String,
    val city: String,
    val state: String,
    val zip: String
)
