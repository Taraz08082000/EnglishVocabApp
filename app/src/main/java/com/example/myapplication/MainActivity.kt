package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.myapplication.data.repository.AppRepository
import com.example.myapplication.ui.navigation.AppNavGraph
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = AppRepository.getInstance(this)

        setContent {
            MyApplicationTheme {
                AppNavGraph(repository = repository)
            }
        }
    }
}
