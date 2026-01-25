package com.example.minimap32.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.minimap32.model.WifiNetwork
import com.example.minimap32.wifi.AppWifiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WifiViewModel(
    application: Application
) : AndroidViewModel(application) {

    // IMPORT WIFI MANAGER
    private val wifiManager = AppWifiManager(application)

    // NETWORKS
    // Private -> modifiable only in viewModel
    private val _networks = MutableStateFlow<List<WifiNetwork>>(emptyList())
    // Public -> expose to UI, read only
    val networks = _networks.asStateFlow()

    // SELECTED NETWORK
    private val _selectedNetwork = MutableStateFlow<WifiNetwork?>(null)
    val selectedNetwork = _selectedNetwork.asStateFlow()

    // ISSCANNING VARIABLE
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    // Limit number scan (since android 9+ -> 4 scans max per min)

    fun scanWifi() {
        _isScanning.value = true
        // Reset before scanning
        _networks.value = emptyList()

        wifiManager.startScan { results ->
            _networks.value = results
            _isScanning.value = false
        }
    }

    fun stopWifi() {

    }

    fun selectNetwork(network: WifiNetwork) {
        _selectedNetwork.value = network
    }

    fun clearSelection() {
        _selectedNetwork.value = null
    }

    override fun onCleared() {
        wifiManager.stop()
        super.onCleared()
    }

    fun resetAll() {
        _selectedNetwork.value = null
        _networks.value = emptyList()
    }

}
