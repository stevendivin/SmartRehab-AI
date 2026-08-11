package com.steven.smartrehab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.steven.smartrehab.camera.CameraScreen
import com.steven.smartrehab.presentation.home.HomeScreen
import com.steven.smartrehab.ui.theme.SmartRehabAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartRehabAITheme {
                CameraScreen()
            }
        }
    }
}