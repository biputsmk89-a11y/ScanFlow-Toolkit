package com.scanflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.scanflow.app.core.theme.ScanFlowTheme
import com.scanflow.app.ui.navigation.ScanFlowNavHost

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ScanFlowTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ScanFlowNavHost()
                }
            }
        }
    }
}
