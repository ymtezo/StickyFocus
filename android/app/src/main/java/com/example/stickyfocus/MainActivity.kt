package com.example.stickyfocus

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.util.UUID
import java.util.concurrent.Executors
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    companion object {
        const val PREFS = "sticky_prefs"
        const val KEY_LATEST_TASK = "latest_task"
    }

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Allow this activity to show on lockscreen and turn screen on
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)

        // Pre-fill with last task if any
        val last = prefs.getString(KEY_LATEST_TASK, "") ?: ""
        taskEdit.setText(last)

        saveButton.setOnClickListener {
            val task = taskEdit.text.toString().trim()
            if (task.isEmpty()) {
                Toast.makeText(this, "タスクを入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Save to SharedPreferences (backwards-compatible)
            prefs.edit().putString(KEY_LATEST_TASK, task).apply()

            // Save to Room DB (background)
            val executor = Executors.newSingleThreadExecutor()
            executor.execute {
                try {
                    val db = AppDatabase.getInstance(applicationContext)
                    val now = System.currentTimeMillis()
                    val entity = TaskEntity(
                        id = UUID.randomUUID().toString(),
                        title = task,
                        createdAt = now,
                        updatedAt = now,
                        source = "unlock_input"
                    )
                    db.taskDao().insert(entity)
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Start overlay service
            val overlayIntent = Intent(this, OverlayService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(overlayIntent)
            } else {
                startService(overlayIntent)
            }

            Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show()
            finish()
        }

        shareButton.setOnClickListener {
            val task = taskEdit.text.toString().trim()
            if (task.isEmpty()) {
                Toast.makeText(this, "タスクが空です", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val share = Intent(Intent.ACTION_SEND)
            share.type = "text/plain"
            share.putExtra(Intent.EXTRA_TEXT, task)
            startActivity(Intent.createChooser(share, "共有"))
        }

        overlayPermissionButton.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "既にオーバーレイ許可が与えられています", Toast.LENGTH_SHORT).show()
            }
        }

        // Start unlock monitor service (so ACTION_USER_PRESENT is observed)
        val unlockIntent = Intent(this, UnlockMonitorService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(unlockIntent)
        } else {
            startService(unlockIntent)
        }
    }
}
