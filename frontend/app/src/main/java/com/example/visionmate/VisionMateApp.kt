package com.example.visionmate

import androidx.compose.runtime.Composable
import com.example.visionmate.navigation.NavGraph
import com.example.visionmate.ui.theme.VisionMateTheme

@Composable
fun VisionMateApp() {
    VisionMateTheme {
        NavGraph()
    }
}
