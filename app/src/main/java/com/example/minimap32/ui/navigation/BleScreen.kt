package com.example.minimap32.ui.navigation


import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.minimap32.ble.BleManager

@SuppressLint("MissingPermission")
@Composable
fun BleScreen() {

    val context = LocalContext.current
    val bleManager = remember { BleManager(context) }

    var cmdQuery by remember { mutableStateOf("") }

    val macEvent by bleManager.macEvents.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = { bleManager.startScan() }) {
            Text("Scan & Connect")
        }

        Button(onClick = { bleManager.stopScan() }) {
            Text("Stop Scan")
        }

        TextField(
            value = cmdQuery,
            onValueChange = { cmdQuery = it },
            label = { Text("Send CMD") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        Button(onClick = { bleManager.sendCommand(cmdQuery) }) {
            Text("Send CMD")
        }

        Button(onClick = { bleManager.clearMacDisplayed() }) {
            Text("Remove current MACs")
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(
                items = macEvent,
                key = { it.mac }   // ⭐ CRITICAL
            ) { evt ->
                Text(
                    text = "${evt.mac} | RSSI ${evt.rssi} | CH ${evt.channel}",
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
