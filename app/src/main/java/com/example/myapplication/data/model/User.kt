package com.example.myapplication.data.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val currentStreak: Int = 1,
    val lastActiveDate: String = "",
    val notificationsEnabled: Boolean = true
)
