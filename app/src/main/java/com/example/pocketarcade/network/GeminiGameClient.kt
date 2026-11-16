package com.example.pocketarcade.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiGameClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun generateHtmlGame(apiKey: String, userPrompt: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                // Wir nutzen hier das stabile, aktuelle Standardmodell, das mit allen neuen AI-Studio-Keys kompatibel ist
                val url =
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

                val systemPrompt = """
                    Du bist ein Experte für HTML5-Canvas-Arcade-Minigames.
                    Erstelle ein vollständiges, sofort spielbares, fehlerfreies Single-File-HTML5-Spiel basierend auf dem Nutzer-Prompt.
                    STRIKTE REGELN:
                    1. Gib AUSSCHLIESSLICH reinen HTML-Code zurück (beginnend mit <!DOCTYPE html> und endend mit </html>). Kein Markdown, keine ```html Code-Blöcke, keine Erklärungen.
                    2. Alle CSS-Styles und JavaScript-Logik müssen inline in dieser einen HTML-Datei enthalten sein. Keine externen Bilder, Fonts, Sounds oder CDNs.
                    3. Das Canvas muss 100vw x 100vh ausfüllen (overflow: hidden, touch-action: none, user-select: none) und auf mobile Touch-Events (touchstart, touchmove, pointerdown) sowie Mausklicks reagieren.
                    4. Nutze einen stylischen dunklen Neon-Look (#05050A Hintergrund, leuchtende Farben).
                    5. Zeige oben links den aktuellen Score an.
                    6. Baue einen klaren Game-Over-Screen direkt auf dem Canvas mit einem Neustart-Button/Tap-to-Restart ein.
                    7. Rufe bei jedem neuen Highscore oder Game-Over sicher folgende Bridge auf, falls vorhanden:
                       if (window.AndroidBridge && window.AndroidBridge.onScoreUpdate) { window.AndroidBridge.onScoreUpdate(Math.floor(score)); }
                """.trimIndent()

                val requestJson = JSONObject().apply {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                    })
                    put("contents", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().put("text", "Erstelle dieses Arcade-Spiel: $userPrompt")))
                    }))
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.85)
                        put("maxOutputTokens", 8192)
                    })
                }

                val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val errorMsg = try {
                            JSONObject(responseBody).optJSONObject("error")?.optString("message")
                        } catch (_: Exception) {
                            null
                        } ?: "HTTP ${response.code}"
                        return@withContext Result.failure(Exception("Gemini API Fehler: $errorMsg"))
                    }

                    val json = JSONObject(responseBody)
                    val candidates = json.optJSONArray("candidates")
                        ?: return@withContext Result.failure(Exception("Keine Antwort von Gemini erhalten."))
                    val text = candidates.getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    val cleanedHtml = stripMarkdownFences(text)
                    if (!cleanedHtml.contains("<canvas", ignoreCase = true) && !cleanedHtml.contains("<html", ignoreCase = true)) {
                        return@withContext Result.failure(Exception("Ungültiges HTML von KI erhalten."))
                    }
                    Result.success(cleanedHtml)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun stripMarkdownFences(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```")) {
            val firstNewline = text.indexOf('\n')
            if (firstNewline != -1) {
                text = text.substring(firstNewline + 1)
            }
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length - 3)
        }
        return text.trim()
    }
}
