package com.example.stickyfocus

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TaskDao {
    @Insert
    fun insert(task: TaskEntity)

    @Update
    fun update(task: TaskEntity)

    @Query("SELECT * FROM tasks ORDER BY isPinned DESC, createdAt DESC LIMIT 1")
    fun getLatestPinnedOrLatest(): TaskEntity?

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    fun getAll(limit: Int, offset: Int): List<TaskEntity>

    @Query("DELETE FROM tasks WHERE id = :taskId")
    fun deleteById(taskId: String)
}
