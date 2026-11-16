package com.example.pocketarcade.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.pocketarcade.generator.GeneratedGameSpec
import com.example.pocketarcade.generator.OfflineGameGenerator
import com.example.pocketarcade.network.GeminiGameClient
import kotlinx.coroutines.flow.Flow

class GameRepository(
    private val context: Context,
    private val gameDao: GameDao,
    private val apiKeyStore: ApiKeyStore,
    private val geminiClient: GeminiGameClient = GeminiGameClient()
) {
    val savedGames: Flow<List<GameEntity>> = gameDao.getAllGames()
    val apiKeyFlow = apiKeyStore.apiKey

    fun saveApiKey(key: String) {
        apiKeyStore.saveApiKey(key)
    }

    suspend fun createGame(promptInput: String, isRandomRequest: Boolean): Pair<GeneratedGameSpec, String?> {
        val effectivePrompt = if (promptInput.isBlank() || isRandomRequest) {
            OfflineGameGenerator.pickRandomPrompt()
        } else {
            promptInput.trim()
        }

        val key = apiKeyStore.getCurrentKey()
        if (key.isNotBlank() && isNetworkAvailable()) {
            val aiResult = geminiClient.generateHtmlGame(key, effectivePrompt)
            aiResult.fold(
                onSuccess = { html ->
                    return Pair(
                        GeneratedGameSpec(
                            title = effectivePrompt.take(36),
                            prompt = effectivePrompt,
                            htmlCode = html,
                            isOffline = false
                        ),
                        null
                    )
                },
                onFailure = { error ->
                    val fallback = OfflineGameGenerator.generateRandomGame(effectivePrompt)
                    return Pair(fallback, "KI-Fallback aktiv (${error.message})")
                }
            )
        } else {
            val reason = if (key.isBlank()) {
                "Offline-Generator aktiv (Kein API-Key hinterlegt)"
            } else {
                "Offline-Generator aktiv (Keine Internetverbindung)"
            }
            return Pair(OfflineGameGenerator.generateRandomGame(effectivePrompt), reason)
        }
    }

    suspend fun saveGameToArcade(spec: GeneratedGameSpec, highScore: Int): Long {
        return gameDao.insertGame(
            GameEntity(
                title = spec.title,
                prompt = spec.prompt,
                htmlCode = spec.htmlCode,
                highScore = highScore,
                isOfflineGenerated = spec.isOffline
            )
        )
    }

    suspend fun updateSavedGameScore(gameId: Long, score: Int) {
        gameDao.updateHighScore(gameId, score)
    }

    suspend fun deleteGame(gameId: Long) {
        gameDao.deleteGame(gameId)
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
