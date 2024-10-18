package com.example.visionmate.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.visionmate.screens.Home
import com.example.visionmate.viewmodel.VisionViewModel

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val viewModel: VisionViewModel = hiltViewModel()
            Home(
                viewModel = viewModel,
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Home : Screen("home")
}