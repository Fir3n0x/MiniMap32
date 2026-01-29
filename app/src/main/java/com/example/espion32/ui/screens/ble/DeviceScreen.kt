package com.example.espion32.ui.screens.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.espion32.autowide
import com.example.espion32.viewmodel.BleViewModel

@SuppressLint("MissingPermission")
@Composable
fun DeviceScreen(navController: NavController, viewModel: BleViewModel) {
    val devices by viewModel.devices.collectAsState()

    // Start scan when screen appears
    LaunchedEffect(Unit) {
        viewModel.startScan()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.bleManager.stopScan()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFCDCDCD))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp)
        ) {
            // return
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp)
                    .background(Color(0xFF1E2624).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .clickable {
                        navController.navigate("login") {
                            popUpTo(0)
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "<",
                    color = Color.White.copy(alpha = 0.9f),
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
                    text = "Select Device",
                    color = Color(0xFF363535),
                    fontFamily = autowide,
                    fontSize = 24.sp
                )
            }
        }

        // Main content
        LazyColumn(
            modifier = Modifier
                .padding(top = 10.dp)
        ) {
            items(devices) { device ->
                DeviceItem(
                    device = device,
                    onClick = {
                        viewModel.selectDevice(device)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceItem(
    device: BluetoothDevice,
    onClick: () -> Unit
) {
    val deviceName = device.name ?: "Unknown device"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.Gray.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, Color(0xFF363535))
    ) {
        Text(
            text = deviceName,
            modifier = Modifier.padding(top = 16.dp, start =  16.dp, bottom = 4.dp),
            color = Color.White.copy(alpha = 0.9f)
        )
        Text(
            text = device.address,
            modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, top = 4.dp),
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}
