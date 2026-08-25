package com.example.data.model

import androidx.room.ColumnInfo

data class CustomerFrequency(
    @ColumnInfo(name = "customer_name")
    val customerName: String,
    @ColumnInfo(name = "order_count")
    val orderCount: Int
)
