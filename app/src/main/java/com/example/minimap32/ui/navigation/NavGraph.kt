package com.example.minimap32.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.minimap32.ble.BleConnectionState
import com.example.minimap32.ui.screens.ConnectedScreen
import com.example.minimap32.ui.screens.ConnectingScreen
import com.example.minimap32.ui.screens.DeviceScreen
import com.example.minimap32.ui.screens.LoginScreen
import com.example.minimap32.viewmodel.BleViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // ViewModel state to share with screens
    val bleViewModel: BleViewModel = viewModel()

    // Track previous route to avoid unnecessary resets
    val previousRoute = remember { mutableStateOf<String?>(null) }

    // Disconnect from BLE only when manually going back to login
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val currentRoute = destination.route
            val wasConnectedOrConnecting = previousRoute.value in listOf("connected", "connecting")

            // Only reset if:
            // 1. Coming to login from connected/connecting screens
            // 2. AND connection is not in error state (error already handles cleanup)
            if (currentRoute == "login" &&
                wasConnectedOrConnecting) {
                bleViewModel.bleManager.resetSession()
                bleViewModel.clearSelection()
            }

            previousRoute.value = currentRoute
        }

        navController.addOnDestinationChangedListener(listener)

        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                navController = navController,
                viewModel = bleViewModel
            )
        }
        composable("devices") {
            DeviceScreen(
                navController = navController,
                viewModel = bleViewModel
            )
        }
        composable("connected") {
            ConnectedScreen(
                navController = navController,
                viewModel = bleViewModel
            )
        }
        composable("connecting") {
            ConnectingScreen(
                navController = navController,
                viewModel = bleViewModel
            )
        }
    }
}