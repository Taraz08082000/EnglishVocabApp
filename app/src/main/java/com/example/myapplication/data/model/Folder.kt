package com.example.myapplication.data.model

data class Folder(
    val id: String,
    val userId: String,
    val name: String,
    val description: String = "",
    val colorHex: String = "#4F46E5",
    val createdAt: Long = System.currentTimeMillis()
)
