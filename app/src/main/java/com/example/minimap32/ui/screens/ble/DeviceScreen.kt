package com.example.minimap32.ui.screens.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.minimap32.viewmodel.BleViewModel

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

    LazyColumn {
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
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF121212)
        ),
        border = BorderStroke(1.dp, Color.White)
    ) {
        Text(
            text = deviceName,
            modifier = Modifier.padding(16.dp),
            color = Color.White
        )
        Text(
            text = device.address,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}
