package com.example.minimap32.ui.screens

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.minimap32.ble.BleConnectionState
import com.example.minimap32.viewmodel.BleViewModel
import kotlinx.coroutines.delay

@SuppressLint("MissingPermission")
@Composable
fun ConnectedScreen(navController: NavController, viewModel: BleViewModel) {

    val state by viewModel.connectionState.collectAsState()

    BackHandler {
        navController.navigate("login") {
            popUpTo(0) // Empty the stack
        }
    }

    LaunchedEffect(state) {
        when (state) {

            is BleConnectionState.Disconnected,
            is BleConnectionState.Error -> {

                // petite pause UX
                delay(1000)

                // reset BLE
                viewModel.bleManager.resetSession()
                viewModel.clearSelection()

                // navigation propre vers login
                navController.navigate("login") {
                    popUpTo("connected") { inclusive = true }
                }
            }

            else -> {}
        }
    }

    /* ===== UI CONNECTED ===== */
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp)
    ) {
        Text(
            text = "CONNECTED",
            color = Color(0xFF00FF00),
            fontFamily = FontFamily.Monospace,
            fontSize = 20.sp
        )

        // ... le reste de ton UI de contrôle ESP32
    }
}