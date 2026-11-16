package com.example.pocketarcade.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ApiKeyStore(context: Context) {
    private val prefs = context.getSharedPreferences("pocket_arcade_prefs", Context.MODE_PRIVATE)
    private val _apiKey = MutableStateFlow(prefs.getString(KEY_GEMINI_API, "").orEmpty())
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    fun saveApiKey(key: String) {
        val cleaned = key.trim()
        prefs.edit().putString(KEY_GEMINI_API, cleaned).apply()
        _apiKey.value = cleaned
    }

    fun getCurrentKey(): String = _apiKey.value

    companion object {
        private const val KEY_GEMINI_API = "gemini_api_key"
    }
}
