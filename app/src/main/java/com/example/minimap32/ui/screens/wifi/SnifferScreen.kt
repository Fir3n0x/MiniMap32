package com.example.minimap32.ui.screens.wifi

import android.Manifest
import android.annotation.SuppressLint
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.minimap32.autowide
import com.example.minimap32.model.Command
import com.example.minimap32.model.WifiNetwork
import com.example.minimap32.viewmodel.BleViewModel
import com.example.minimap32.viewmodel.WifiViewModel

@SuppressLint("MissingPermission")
@Composable
fun SnifferScreen(navController: NavController, bleViewModel: BleViewModel, wifiViewModel: WifiViewModel) {

    // WIFI Variables
    val selectedNetwork by wifiViewModel.selectedNetwork.collectAsState()

    // Deauth state
    var attackLogs by remember { mutableStateOf(listOf<String>()) }
    var detectedMacs by remember { mutableStateOf(listOf<Pair<String, Int>>()) } // MAC & RSSI
    var isAttackRunning by remember { mutableStateOf(false) }
    var safetyCheckbox by remember { mutableStateOf(false) }

    // Content box
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            // Return to login page
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp)
                    .background(Color(0xFF232222), RoundedCornerShape(8.dp))
                    .clickable {
                        navController.navigate("connected") {
                            popUpTo("sniffer") { inclusive = true }
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
                    text = "Sniffer Panel",
                    color = Color.Green,
                    fontFamily = autowide,
                    fontSize = 24.sp
                )
            }
        }

        // Display targeted network
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

            // Display the targeted network
            DisplayTargetedNetwork(selectedNetwork)

            Spacer(Modifier.height(24.dp))

            // Wifi Info collapsible
            WifiInfoSection(selectedNetwork)

            Spacer(Modifier.height(32.dp))

            // Main content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // LEFT COLUMN - Target MAC & Attack Logs
                Column(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight()
                ) {
                    // Attack Logs Terminal
                    Text(
                        text = "Attack Logs",
                        color = Color.Green,
                        fontFamily = autowide,
                        fontSize = 12.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF0A0A0A), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF1E2624), RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        LazyColumn {
                            items(attackLogs) { log ->
                                Text(
                                    text = "> $log",
                                    color = Color(0xFF00FF00),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // RIGHT COLUMN - Detected MAC Addresses
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = "Detected MACs",
                        color = Color.Green,
                        fontFamily = autowide,
                        fontSize = 12.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(Color(0xFF1A1A1A), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (detectedMacs.isEmpty()) {
                                item {
                                    Text(
                                        text = "Sniff packet to get MAC",
                                        color = Color.Gray,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                items(detectedMacs) { (mac, rssi) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF0F0F0F), RoundedCornerShape(4.dp))
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = mac,
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = "$rssi dBm",
                                            color = Color(0xFF00FF00),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Bottom Controls: Launch Button & Safety Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Launch/Stop Button
                Box(
                    modifier = Modifier
                        .background(
                            if (isAttackRunning) Color(0xFFCC0000) else Color(0xFF1E2624),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = safetyCheckbox) {
                            isAttackRunning = !isAttackRunning
                            if (isAttackRunning) {
                                // START ATTACK
                                attackLogs = attackLogs + "Attack started on ${selectedNetwork?.ssid}"
                                launchSnifferAttack(bleViewModel, selectedNetwork)
                            } else {
                                // STOP ATTACK
                                attackLogs = attackLogs + "Attack stopped"
                                stopSnifferAttack(bleViewModel)
                            }
                        }
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = if (isAttackRunning) "STOP ATTACK" else "LAUNCH ATTACK",
                        color = if (safetyCheckbox) Color.Green else Color.Gray,
                        fontFamily = autowide,
                        fontSize = 16.sp
                    )
                }

                Spacer(Modifier.width(16.dp))

                // Safety Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { safetyCheckbox = !safetyCheckbox }
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                if (safetyCheckbox) Color(0xFF00FF00) else Color(0xFF1A1A1A),
                                RoundedCornerShape(4.dp)
                            )
                            .border(1.dp, Color(0xFF1E2624), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (safetyCheckbox) {
                            Text("X", color = Color.Black, fontSize = 16.sp)
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "Safety",
                        color = Color.Green,
                        fontFamily = autowide,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun launchSnifferAttack(bleViewModel: BleViewModel, selectedNetwork: WifiNetwork?) {
    if(selectedNetwork == null) return

    bleViewModel.bleManager.sendCommand(
        Command.SendSniffStart(
            ssid = selectedNetwork.ssid,
            bssid = selectedNetwork.bssid,
            channel = selectedNetwork.channel
        )
    )
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun stopSnifferAttack(bleViewModel: BleViewModel) {
    bleViewModel.bleManager.sendCommand(
        Command.SendSniffStop
    )
}