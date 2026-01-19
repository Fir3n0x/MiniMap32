package com.example.minimap32.ui.screens.ble

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
fun ConnectingScreen(navController: NavController, viewModel: BleViewModel) {

    val state by viewModel.connectionState.collectAsState()
    var hasNavigated by rememberSaveable { mutableStateOf(false) }

    fun statusFor(step: BleConnectionState): LineStatus =
        when {
            state is BleConnectionState.Error -> LineStatus.ERROR
            state::class == step::class ||
                    state is BleConnectionState.Ready -> LineStatus.SUCCESS
            else -> LineStatus.PENDING
        }

    // Only connect once when screen appears
    LaunchedEffect(Unit) {
        hasNavigated = false
        viewModel.connectToSelectedDevice()
    }

    // Handle navigation based on connection state
    LaunchedEffect(state) {
        // Prevent multiple navigations
        if (hasNavigated) return@LaunchedEffect

        when (state) {
            is BleConnectionState.Ready -> {
                hasNavigated = true
                navController.navigate("connected") {
                    popUpTo("login") { inclusive = false }
                }
            }

            is BleConnectionState.Error -> {
                hasNavigated = true
                val errorMsg = (state as BleConnectionState.Error)

                // Wait before coming back to login page
                delay(6000)

                // Navigate back to login WITHOUT calling resetSession here
                // (it will be handled by AppNavigation if needed)
                navController.navigate("login") {
                    popUpTo("connecting") { inclusive = true }
                }
            }

            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(top = 170.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(24.dp)
        ) {

            // title
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = "Connecting...",
                    color = Color.Green,
                    fontFamily = autowide,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Display information
            HackerLine(
                "Connecting to device…",
                statusFor(BleConnectionState.Connected)
            )
            HackerLine(
                "MTU negotiation…",
                statusFor(BleConnectionState.MtuRequested)
            )
            HackerLine(
                "Discovering services…",
                statusFor(BleConnectionState.ServicesDiscovered)
            )
            HackerLine(
                "Device ready",
                statusFor(BleConnectionState.Ready)
            )

            // Handle error message
            if (state is BleConnectionState.Error) {
                // Show error message if error occurred
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Error: ${(state as BleConnectionState.Error)}",
                    color = Color.Red,
                    fontSize = 12.sp,
                    fontFamily = autowide
                )

                // Show retry button on error
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        hasNavigated = true
                        navController.navigate("login") {
                            popUpTo("connecting") { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 24.dp)
                        .border(1.dp, Color(0xFF00FF00)),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF121212),
                        contentColor = Color(0xFF00FF00)
                    )
                ) {
                    Text(
                        text = "BACK TO LOGIN",
                        fontFamily = autowide,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

enum class LineStatus {
    PENDING,
    SUCCESS,
    ERROR
}

@SuppressLint("MissingPermission")
@Composable
fun HackerLine(text: String, status: LineStatus) {

    val color = when (status) {
        LineStatus.PENDING -> Color.Gray
        LineStatus.SUCCESS -> Color(0xFF00FF00)
        LineStatus.ERROR -> Color.Red
    }

    val prefix = when (status) {
        LineStatus.PENDING -> "[ .. ]"
        LineStatus.SUCCESS -> "[ OK ]"
        LineStatus.ERROR -> "[ !! ]"
    }

    Text(
        text = "$prefix $text",
        color = color,
        fontSize = 14.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}