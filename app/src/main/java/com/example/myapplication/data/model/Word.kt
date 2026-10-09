package com.example.myapplication.data.model

data class Word(
    val id: String,
    val folderId: String,
    val english: String,
    val kazakh: String,
    val exampleSentence: String = "",
    val isLearned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
