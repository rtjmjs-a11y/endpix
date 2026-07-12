package com.example.endpix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.endpix.ui.EditorScreen
import com.example.endpix.ui.EditorViewModel
import com.example.endpix.ui.theme.EndpixTheme

class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EndpixTheme(darkTheme = true, dynamicColor = false) {
                EditorScreen(viewModel)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.glView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        viewModel.glView?.onResume()
    }
}
