package com.decisionpro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.decisionpro.app.ui.navigation.AppRoot
import com.decisionpro.app.ui.theme.DecisionMakerTheme
import dagger.hilt.android.Android

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            DecisionMakerTheme {
                AppRoot()
            }
        }
    }
}
