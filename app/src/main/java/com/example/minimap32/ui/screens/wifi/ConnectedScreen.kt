package com.example.minimap32.ui.screens.wifi

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.minimap32.autowide
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

                // Break UX
                delay(1000)

                // reset BLE
                viewModel.bleManager.resetSession()
                viewModel.clearSelection()

                // navigation to login
                navController.navigate("login") {
                    popUpTo("connected") { inclusive = true }
                }
            }

            else -> {}
        }
    }

    // Content box
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
    ){
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .align(Alignment.TopCenter)
        ) {
            // return
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clickable {
                        navController.navigate("login") {
                            popUpTo(0)
                        }
                    }
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = "<",
                    color = Color.Green,
                    fontFamily = autowide,
                    fontSize = 35.sp
                )
            }

            // title
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = "Control Panel",
                    color = Color.Green,
                    fontFamily = autowide,
                    fontSize = 24.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(top = 80.dp)
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "CONNECTED",
                color = Color(0xFF00FF00),
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp
            )

        }
    }
}