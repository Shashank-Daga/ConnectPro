package com.connectpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.connectpro.ui.navigation.ConnectProApp
import com.connectpro.ui.theme.ConnectProTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            ConnectProTheme {
                ConnectProApp()
            }
        }
    }
}
