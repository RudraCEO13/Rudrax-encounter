package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.MotionXRootApp
import com.example.ui.theme.RudraXMotionXTheme
import com.example.viewmodel.MotionXViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            RudraXMotionXTheme {
                val motionViewModel: MotionXViewModel = viewModel()
                MotionXRootApp(viewModel = motionViewModel)
            }
        }
    }
}
