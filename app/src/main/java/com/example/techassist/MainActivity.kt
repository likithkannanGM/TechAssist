package com.example.techassist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.techassist.core.networkStatusFlow
import com.example.techassist.navigation.TechAssistNavHost
import com.example.techassist.ui.theme.TechAssistTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TechAssistTheme {
                TechAssistNavHost(isOnlineFlow = networkStatusFlow())
            }
        }
    }
}