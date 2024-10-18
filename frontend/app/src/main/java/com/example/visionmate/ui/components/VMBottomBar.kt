package com.example.visionmate.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiObjects
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Sick
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun VMBottomBar(
    onRecognizeTextClick: () -> Unit,
) {

    val selectedButton = remember { mutableStateOf("Text Recognition") }

    BottomAppBar {
        Box(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center
            ) {
                FloatingActionButton(
                    onClick = {
                        selectedButton.value = "Text Recognition"
                        onRecognizeTextClick()
                    },
                    containerColor = getButtonColor(selectedButton.value, "Text Recognition"),
                    elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(Icons.Filled.TextFormat, "Text Recognition")
                }
            }
        }
    }
}

@Composable
fun getButtonColor(selectedButton: String, buttonLabel: String): Color {
    return if (selectedButton == buttonLabel) {
        BottomAppBarDefaults.bottomAppBarFabColor
    } else {
        BottomAppBarDefaults.bottomAppBarFabColor.copy(alpha = 0.5f)
    }
}

@Preview
@Composable
fun VMBottomBarPreview() {
    VMBottomBar(
        onRecognizeTextClick = { },
    )
}