package com.example.pocketarcade.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val prompt: String,
    val htmlCode: String,
    val highScore: Int = 0,
    val isOfflineGenerated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
