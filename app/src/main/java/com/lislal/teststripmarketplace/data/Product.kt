package com.lislal.teststripmarketplace.data

data class Product(
    val barcode: String,
    val category: String,
    val description: String,
    var prices: List<Int>
)
