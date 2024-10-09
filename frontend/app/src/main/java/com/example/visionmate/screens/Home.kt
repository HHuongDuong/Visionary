package com.example.visionmate.screens

import android.annotation.SuppressLint
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.visionmate.ui.components.CameraPermissionHandler
import com.example.visionmate.ui.components.VMBottomBar
import com.example.visionmate.ui.components.VMTopBar

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun Home() {
    Scaffold(
        topBar = { VMTopBar() },
        bottomBar = { VMBottomBar() }
    ) {
        CameraPermissionHandler()
    }
}

@Preview
@Composable
fun HomePreview() {
    Home()
}