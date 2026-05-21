package com.aegisnet.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import com.aegisnet.mobile.ui.navigation.AegisNavGraph
import com.aegisnet.mobile.ui.theme.AegisNetTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            AegisNetTheme {
                AegisNavGraph()
            }
        }
    }
}
