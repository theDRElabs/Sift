package com.example.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationHelper {

    fun scheduleTodoNotification(context: Context, itemId: Int, content: String, dueAt: Long) {
        val delay = dueAt - System.currentTimeMillis()
        if (delay <= 0) return // Already past due

        val inputData = Data.Builder()
            .putInt("item_id", itemId)
            .putString("content", content)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<TodoNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "todo_$itemId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelTodoNotification(context: Context, itemId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork("todo_$itemId")
    }
}
