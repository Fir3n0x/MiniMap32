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

    private val wifiManager = AppWifiManager(application)

    // Private -> modifiable only in viewModel
    private val _networks = MutableStateFlow<List<WifiNetwork>>(emptyList())
    // Public -> expose to UI, read only
    val networks = _networks.asStateFlow()

    private val _selectedNetwork = MutableStateFlow<WifiNetwork?>(null)
    val selectedNetwork = _selectedNetwork.asStateFlow()

    fun scanWifi() {
        wifiManager.startScan { results ->
            _networks.value = results
        }
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
}
