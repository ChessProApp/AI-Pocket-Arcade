package com.example.pocketarcade.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketarcade.data.ApiKeyStore
import com.example.pocketarcade.data.AppDatabase
import com.example.pocketarcade.data.GameEntity
import com.example.pocketarcade.data.GameRepository
import com.example.pocketarcade.generator.GeneratedGameSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActiveGameSession(
    val savedId: Long?, // null = neu generiert (noch nicht gespeichert)
    val spec: GeneratedGameSpec,
    val currentSessionBestScore: Int = 0
)

data class ArcadeUiState(
    val promptInput: String = "",
    val isGenerating: Boolean = false,
    val statusBanner: String? = null,
    val activeSession: ActiveGameSession? = null,
    val showSaveDialog: Boolean = false,
    val showApiKeyDialog: Boolean = false
)

class ArcadeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameRepository(
        context = application,
        gameDao = AppDatabase.getInstance(application).gameDao(),
        apiKeyStore = ApiKeyStore(application)
    )

    val savedGames: StateFlow<List<GameEntity>> = repository.savedGames.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val apiKey: StateFlow<String> = repository.apiKeyFlow

    private val _uiState = MutableStateFlow(ArcadeUiState())
    val uiState: StateFlow<ArcadeUiState> = _uiState.asStateFlow()

    fun onPromptChanged(newPrompt: String) {
        _uiState.update { it.copy(promptInput = newPrompt) }
    }

    fun openApiKeyDialog() {
        _uiState.update { it.copy(showApiKeyDialog = true) }
    }

    fun closeApiKeyDialog() {
        _uiState.update { it.copy(showApiKeyDialog = false) }
    }

    fun saveApiKey(key: String) {
        repository.saveApiKey(key)
        _uiState.update {
            it.copy(
                showApiKeyDialog = false,
                statusBanner = if (key.isBlank()) "API-Key entfernt" else "Gemini API-Key gespeichert"
            )
        }
    }

    fun clearStatusBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }

    fun generateGame(isRandom: Boolean) {
        if (_uiState.value.isGenerating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, statusBanner = null) }
            val (spec, banner) = repository.createGame(_uiState.value.promptInput, isRandom)
            _uiState.update {
                it.copy(
                    isGenerating = false,
                    statusBanner = banner,
                    activeSession = ActiveGameSession(savedId = null, spec = spec, currentSessionBestScore = 0),
                    showSaveDialog = false
                )
            }
        }
    }

    fun launchSavedGame(entity: GameEntity) {
        _uiState.update {
            it.copy(
                activeSession = ActiveGameSession(
                    savedId = entity.id,
                    spec = GeneratedGameSpec(
                        title = entity.title,
                        prompt = entity.prompt,
                        htmlCode = entity.htmlCode,
                        isOffline = entity.isOfflineGenerated
                    ),
                    currentSessionBestScore = entity.highScore
                ),
                showSaveDialog = false
            )
        }
    }

    fun onScoreReported(score: Int) {
        val session = _uiState.value.activeSession ?: return
        val newBest = maxOf(session.currentSessionBestScore, score)
        _uiState.update {
            it.copy(activeSession = session.copy(currentSessionBestScore = newBest))
        }
        val savedId = session.savedId
        if (savedId != null && score > 0) {
            viewModelScope.launch {
                repository.updateSavedGameScore(savedId, score)
            }
        }
    }

    fun requestExitGame() {
        val session = _uiState.value.activeSession ?: return
        if (session.savedId == null) {
            _uiState.update { it.copy(showSaveDialog = true) }
        } else {
            _uiState.update { it.copy(activeSession = null, showSaveDialog = false) }
        }
    }

    fun confirmSaveAndExit(customTitle: String) {
        val session = _uiState.value.activeSession ?: return
        val finalTitle = customTitle.trim().ifBlank { session.spec.title }
        viewModelScope.launch {
            repository.saveGameToArcade(
                spec = session.spec.copy(title = finalTitle),
                highScore = session.currentSessionBestScore
            )
            _uiState.update {
                it.copy(
                    activeSession = null,
                    showSaveDialog = false,
                    statusBanner = "„$finalTitle“ in deiner Arcade gespeichert!"
                )
            }
        }
    }

    fun discardAndExit() {
        _uiState.update {
            it.copy(activeSession = null, showSaveDialog = false)
        }
    }

    fun dismissSaveDialog() {
        _uiState.update { it.copy(showSaveDialog = false) }
    }

    fun deleteSavedGame(gameId: Long) {
        viewModelScope.launch {
            repository.deleteGame(gameId)
        }
    }
}
