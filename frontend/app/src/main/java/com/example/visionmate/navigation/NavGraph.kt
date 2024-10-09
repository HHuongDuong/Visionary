package com.example.visionmate.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.visionmate.screens.Home

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            Home()
        }
        // Add other composable destinations here
    }
}

sealed class Screen(val route: String) {
    object Home : Screen("home")
    // Add other screens here
}