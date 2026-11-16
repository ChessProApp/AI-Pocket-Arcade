package com.example.pocketarcade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketarcade.ui.ArcadeViewModel
import com.example.pocketarcade.ui.components.ApiKeyDialog
import com.example.pocketarcade.ui.components.SaveGameDialog
import com.example.pocketarcade.ui.screens.ArcadeHubScreen
import com.example.pocketarcade.ui.screens.GamePlayerScreen
import com.example.pocketarcade.ui.theme.PocketArcadeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketArcadeTheme {
                val viewModel: ArcadeViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()
                val savedGames by viewModel.savedGames.collectAsState()
                val apiKey by viewModel.apiKey.collectAsState()

                val activeSession = uiState.activeSession

                if (activeSession != null) {
                    GamePlayerScreen(
                        session = activeSession,
                        onScoreUpdate = viewModel::onScoreReported,
                        onExitRequested = viewModel::requestExitGame
                    )

                    if (uiState.showSaveDialog) {
                        SaveGameDialog(
                            defaultTitle = activeSession.spec.title,
                            highScore = activeSession.currentSessionBestScore,
                            onSave = viewModel::confirmSaveAndExit,
                            onDiscard = viewModel::discardAndExit,
                            onCancel = viewModel::dismissSaveDialog
                        )
                    }
                } else {
                    ArcadeHubScreen(
                        promptInput = uiState.promptInput,
                        isGenerating = uiState.isGenerating,
                        hasApiKey = apiKey.isNotBlank(),
                        statusBanner = uiState.statusBanner,
                        savedGames = savedGames,
                        onPromptChange = viewModel::onPromptChanged,
                        onGenerateFromPrompt = { viewModel.generateGame(isRandom = false) },
                        onGenerateRandom = { viewModel.generateGame(isRandom = true) },
                        onOpenSettings = viewModel::openApiKeyDialog,
                        onPlaySavedGame = viewModel::launchSavedGame,
                        onDeleteSavedGame = viewModel::deleteSavedGame
                    )

                    if (uiState.showApiKeyDialog) {
                        ApiKeyDialog(
                            currentKey = apiKey,
                            onSave = viewModel::saveApiKey,
                            onDismiss = viewModel::closeApiKeyDialog
                        )
                    }
                }
            }
        }
    }
}
