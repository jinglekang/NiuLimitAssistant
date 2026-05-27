package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.OperationLogRepository
import com.example.ui.MainScreen
import com.example.ui.NiuViewModel
import com.example.ui.theme.NiuLimitAssistantTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val dao = database.operationLogDao()
        val repository = OperationLogRepository(dao)

        val viewModelFactory = NiuViewModel.Factory(applicationContext, repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[NiuViewModel::class.java]

        setContent {
            NiuLimitAssistantTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
