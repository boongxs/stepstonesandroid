package com.flutter.stepstonesflt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.flutter.stepstonesflt.ui.screen.MainScreen
import com.flutter.stepstonesflt.ui.theme.StepstonesFltTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StepstonesFltTheme {
                MainScreen()
            }
        }
    }
}
