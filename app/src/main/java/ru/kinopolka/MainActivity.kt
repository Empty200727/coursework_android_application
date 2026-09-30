package ru.kinopolka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import ru.kinopolka.core.ui.theme.KinopolkaTheme
import ru.kinopolka.navigation.KinopolkaApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            KinopolkaTheme {
                KinopolkaApp()
            }
        }
    }
}
