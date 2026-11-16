package com.example.pocketarcade.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM saved_games ORDER BY createdAt DESC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity): Long

    @Query("UPDATE saved_games SET highScore = MAX(highScore, :score) WHERE id = :gameId")
    suspend fun updateHighScore(gameId: Long, score: Int)

    @Query("DELETE FROM saved_games WHERE id = :gameId")
    suspend fun deleteGame(gameId: Long)
}
