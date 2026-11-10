package com.x.player

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.x.player.ui.XRoot
import com.x.player.ui.theme.XTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            XTheme { XRoot() }
        }
    }

    override fun onDestroy() {
        // Playback continues in PlaybackService; we only drop our controller on a real exit.
        if (isFinishing) (application as XApp).container.player.disconnect()
        super.onDestroy()
    }
}
