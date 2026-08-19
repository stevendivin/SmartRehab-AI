package com.steven.smartrehab.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseName: String,
    val date: Long,           // timestamp (System.currentTimeMillis())
    val durationSeconds: Int,
    val totalReps: Int,
    val correctReps: Int,
    val incorrectReps: Int,
    val averageAmplitude: Double
)