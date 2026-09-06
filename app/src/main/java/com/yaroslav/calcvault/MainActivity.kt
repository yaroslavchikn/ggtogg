package com.yaroslav.calcvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yaroslav.calcvault.ui.theme.CalcVaultTheme
import com.yaroslav.calcvault.ui.CalculatorScreen
import com.yaroslav.calcvault.ui.VaultScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalcVaultTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "calculator") {
                        composable("calculator") {
                            CalculatorScreen(onSecretTriggered = { navController.navigate("vault") })
                        }
                        composable("vault") {
                            VaultScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
