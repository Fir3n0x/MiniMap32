package com.example.minimap32.ui.screens.wifi

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
    ){
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            // return
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp)
                    .background(Color(0xFF232222), RoundedCornerShape(8.dp))
                    .clickable {
                        navController.navigate("login") {
                            popUpTo(0)
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 2.dp)
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

//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(Color.Black)
//                .padding(top = 80.dp)
//                .padding(horizontal = 24.dp)
//        ) {
//            Text(
//                text = "CONNECTED",
//                color = Color(0xFF00FF00),
//                fontFamily = FontFamily.Monospace,
//                fontSize = 20.sp
//            )
//        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Target Network
            Text(
                text = "Target Network",
                color = Color.Green,
                fontFamily = autowide,
                fontSize = 16.sp
            )

            Spacer(Modifier.height(12.dp))

            TargetNetworkRow()

            Spacer(Modifier.height(24.dp))

            // Wifi Info collapsible
            WifiInfoSection()

            Spacer(Modifier.height(32.dp))

            // Actions
            Text(
                text = "Actions",
                color = Color.Green,
                fontFamily = autowide,
                fontSize = 16.sp
            )

            Spacer(Modifier.height(16.dp))

            ActionsRow(navController)
        }
    }
}

@Composable
fun TargetNetworkRow() {
    var expanded by remember { mutableStateOf(false) }
    var selectedNetwork by remember { mutableStateOf<String?>(null) }

    // Fake data for now
    val networks = listOf("HomeWifi", "OfficeNet", "ESP32_AP", "PublicWifi")

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {


        // Dropdown
        Box(
            modifier = Modifier
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(6.dp))
                    .clickable { expanded = true }
                    .padding(12.dp)
            ) {
                Text(
                    text = selectedNetwork ?: "Select network...",
                    color = if (selectedNetwork == null) Color.Gray else Color.White,
                    fontFamily = autowide,
                    fontSize = 14.sp
                )
            }




            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                networks.forEach { network ->
                    DropdownMenuItem(
                        text = { Text(network) },
                        onClick = {
                            selectedNetwork = network
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // Clear button
        SmallIconButton(
            label = "X",
            onClick = { selectedNetwork = null }
        )





        Spacer(Modifier.width(8.dp))

        // Refresh button
        SmallIconButton(
            label = "↻",
            onClick = {
                // TODO: callback wifi scan
            }
        )
    }
}

@Composable
fun SmallIconButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(Color(0xFF232222), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.Green,
            fontSize = 16.sp,
            fontFamily = autowide
        )
    }
}


@Composable
fun WifiInfoSection() {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF121212), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {


        // Header clickable
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (expanded) "v Wifi Info" else "> Wifi Info",
                color = Color.Green,
                fontFamily = autowide,
                fontSize = 14.sp
            )
        }

        if (expanded) {
            Spacer(Modifier.height(12.dp))

            Text("SSID: ESP32_AP", color = Color.White, fontSize = 12.sp)
            Text("RSSI: -62 dBm", color = Color.White, fontSize = 12.sp)
            Text("Security: WPA2", color = Color.White, fontSize = 12.sp)
            Text("Channel: 6", color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
fun ActionsRow(navController: NavController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        ActionButton("Sniffer") {
            navController.navigate("sniffer")
        }

        ActionButton("Deauth") {
            navController.navigate("deauth")
        }

        ActionButton("Beacon") {
            navController.navigate("beacon")
        }
    }
}

@Composable
fun ActionButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(Color(0xFF1E2624), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = Color.Green,
            fontFamily = autowide,
            fontSize = 14.sp
        )
    }
}

