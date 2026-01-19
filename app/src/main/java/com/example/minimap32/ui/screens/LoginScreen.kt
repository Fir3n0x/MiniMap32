package com.example.minimap32.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.minimap32.viewmodel.BleViewModel

@SuppressLint("MissingPermission")
@Composable
fun LoginScreen(navController: NavController, viewModel: BleViewModel) {
    val selectedDevices by viewModel.selectedDevice.collectAsState()

    val deviceName = when {
        selectedDevices == null -> "Select Device"
        selectedDevices!!.name.isNullOrBlank() -> "Unknown device"
        else -> selectedDevices!!.name
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "MINIMAP32",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )


        Spacer(Modifier.height(48.dp))

        Button(
            onClick = { navController.navigate("devices") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                deviceName
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                selectedDevices?.let {
                    navController.navigate("connecting")
                }
            },
            enabled = selectedDevices != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect")
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun LoginScreenPreview(){
//    val navController : NavController = rememberNavController()
//    LoginScreen(navController, viewModel = null)
//}