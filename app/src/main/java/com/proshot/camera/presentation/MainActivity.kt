package com.proshot.camera.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.proshot.camera.presentation.camera.CameraScreen
import com.proshot.camera.presentation.theme.ProShotTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            ProShotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    CameraScreen()
                }
            }
        }
    }
}
