package com.coc.upgrade.manager.ui

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.coc.upgrade.manager.ui.theme.BgDark
import com.coc.upgrade.manager.ui.theme.COCUpgradeManagerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle Widget Actions
        val autoProcess = intent.getBooleanExtra("auto_process", false)
        if (autoProcess) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString()
                if (!text.isNullOrBlank()) {
                    viewModel.processJsonImport(text)
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            COCUpgradeManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDark
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
