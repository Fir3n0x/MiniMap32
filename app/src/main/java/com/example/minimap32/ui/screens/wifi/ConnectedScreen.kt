package com.example.minimap32.ui.screens.wifi

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
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
import com.example.minimap32.model.WifiNetwork
import com.example.minimap32.model.displayName
import com.example.minimap32.viewmodel.BleViewModel
import com.example.minimap32.viewmodel.WifiViewModel
import kotlinx.coroutines.delay

@SuppressLint("MissingPermission")
@Composable
fun ConnectedScreen(navController: NavController, bleViewModel: BleViewModel, wifiViewModel: WifiViewModel) {

    // BLE Variables
    val state by bleViewModel.connectionState.collectAsState()

    // WIFI Variables
    val networks by wifiViewModel.networks.collectAsState()
    val selected by wifiViewModel.selectedNetwork.collectAsState()

    // Expand wifi info
    var expanded by remember { mutableStateOf(false) }

    BackHandler {
        navController.navigate("login") {
            popUpTo(0) // Empty the stack
        }
    }

    // Launch wifi scan when the page is displaying
    LaunchedEffect(Unit) {
        wifiViewModel.scanWifi()
        // Stop attack
        stopSnifferAttack(bleViewModel)
        stopDeauthAttack(bleViewModel)
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
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(12.dp))

            TargetNetworkRow(wifiViewModel, networks, selected)

            Spacer(Modifier.height(24.dp))

            // Wifi Info collapsible
            WifiInfoSection(selected)

            Spacer(Modifier.height(32.dp))

            // Actions
            Text(
                text = "Actions",
                color = Color.Green,
                fontFamily = autowide,
                fontSize = 16.sp
            )

            Spacer(Modifier.height(16.dp))

            ActionsRow(navController, selected)
        }
    }
}

@Composable
fun TargetNetworkRow(
    wifiViewModel: WifiViewModel,
    networks: List<WifiNetwork>,
    selected: WifiNetwork?
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedNetwork by remember { mutableStateOf<String?>(null) }
    val isScanning by wifiViewModel.isScanning.collectAsState()

    // Observe refresh button
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(selected) {
        if (selected == null) {
            expanded = false
        }
    }


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Dropdown
        Box(
            modifier = Modifier
                .weight(1f)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(6.dp))
                .clickable { expanded = true }
                .padding(12.dp)
        ) {
            Text(
                text = selected?.displayName() ?: "Select a network",
                color = Color.White,
                fontFamily = autowide
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .heightIn(max = 250.dp)
            ) {
                when {
                    isScanning -> {
                        DropdownMenuItem(
                            text = { Text("Scanning...", color = Color.Gray) },
                            onClick = {}
                        )
                    }

                    networks.isEmpty() -> {
                        DropdownMenuItem(
                            text = { Text("No network found", color = Color.Gray) },
                            onClick = {}
                        )
                    }

                    else -> {
                        networks.forEach { net ->
                            DropdownMenuItem(
                                text = { Text(net.displayName()) },
                                onClick = {
                                    wifiViewModel.selectNetwork(net)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Clear button
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .background(Color(0xFF232222), RoundedCornerShape(6.dp))
                .clickable { wifiViewModel.clearSelection() }
                .padding(10.dp)
        ) {
            Text("X", color = Color.Red)
        }

        // Refresh button
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .background(if (isPressed) Color(0xFF3A3A3A) else Color(0xFF232222), RoundedCornerShape(6.dp))
                .clickable (
                    interactionSource = interactionSource,
                    indication = null
                ){ wifiViewModel.scanWifi() }
                .padding(10.dp)
        ) {
            Text("↻", color = if(isPressed) Color.Yellow else Color.Green)
        }

    }

}

@Composable
fun WifiInfoSection(selected: WifiNetwork?) {

    var infoExpanded by remember { mutableStateOf(true) }

    Spacer(Modifier.height(24.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { infoExpanded = !infoExpanded }
    ) {
        Text(
            text = "> Wifi Info",
            color = Color.Green,
            fontFamily = FontFamily.Monospace
        )
    }

    if (infoExpanded && selected != null) {
        AnimatedVisibility(visible = true) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .background(Color(0xFF121212), RoundedCornerShape(6.dp))
            ) {
                Text("SSID: ${selected.ssid}", color = Color.White)
                Text("BSSID: ${selected.bssid}", color = Color.White)
                Text("RSSI: ${selected.level} dBm", color = Color.White)
                Text("Frequency: ${selected.frequency} MHz", color = Color.White)
                Text("Channel: ${selected.channel}", color = Color.White)
            }
        }
    }
}


@Composable
fun ActionsRow(navController: NavController, selected: WifiNetwork?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        if(selected != null) {
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

@Composable
fun DisplayTargetedNetwork(
    selected: WifiNetwork?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .background(Color(0xFF121212), RoundedCornerShape(6.dp))
    ) {
        Text(
            text = if(selected?.ssid != null) selected.displayName() else "Unknown network",
            color = Color.White,
            fontFamily = autowide,
            fontSize = 14.sp
        )
    }
}


