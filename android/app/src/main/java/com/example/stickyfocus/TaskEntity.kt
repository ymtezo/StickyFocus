package com.example.stickyfocus

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val source: String = "unlock_input",
    val foregroundAppWhenCreated: String? = null,
    val isCompleted: Boolean = false,
    val isPinned: Boolean = false,
    val externalId: String? = null,
    val metadata: String? = null
)
