package com.example.stickyfocus

import android.content.Context
import java.util.concurrent.Executors
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TaskEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sticky_db"
                ).build()
                INSTANCE = instance

                // Migrate from SharedPreferences if needed (simple one-time copy)
                Executors.newSingleThreadExecutor().execute {
                    try {
                        val prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
                        val last = prefs.getString(MainActivity.KEY_LATEST_TASK, null)
                        if (!last.isNullOrEmpty()) {
                            val now = System.currentTimeMillis()
                            val t = TaskEntity(
                                id = java.util.UUID.randomUUID().toString(),
                                title = last,
                                createdAt = now,
                                updatedAt = now,
                                source = "migrated_prefs"
                            )
                            instance.taskDao().insert(t)
                            // Optionally clear the old pref after migration
                            prefs.edit().remove(MainActivity.KEY_LATEST_TASK).apply()
                        }
                    } catch (e: Exception) {
                        // ignore migration errors
                    }
                }

                instance
            }
        }
    }
}
