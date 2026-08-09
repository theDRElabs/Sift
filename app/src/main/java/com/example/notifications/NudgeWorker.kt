package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.Item
import com.example.data.ItemType

class NudgeWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.itemDao()
        
        val items = dao.getAllItemsSync()
        val now = System.currentTimeMillis()
        
        // Filter unresolved, score >= 7, not nudged recently
        val nudgableItems = items.filter { item ->
            val unresolved = if (item.type == ItemType.TODO) !item.isDone else !item.dismissed
            val highScore = item.effectiveScore >= 7
            val notNudgedRecently = item.lastNudgedAt == null || (now - item.lastNudgedAt) > 12L * 60 * 60 * 1000 // 12 hours
            
            unresolved && highScore && notNudgedRecently
        }.sortedByDescending { it.effectiveScore }
        
        if (nudgableItems.isNotEmpty()) {
            val topItems = nudgableItems.take(3)
            sendNudgeNotification(applicationContext, topItems)
            
            // Update lastNudgedAt
            topItems.forEach { item ->
                dao.updateItem(item.copy(lastNudgedAt = now))
            }
        }
        
        return Result.success()
    }
    
    private fun sendNudgeNotification(context: Context, items: List<Item>) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "sift_nudge_channel"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sift Nudges",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Proactive nudges for important items"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // We want to open Important-sorted list. 
            // The viewmodel handles this if we pass an extra or set it in prefs, but currently prefs are used.
            // We can just rely on the user opening the app.
        }
        
        // We'll update prefs to sort by IMPORTANT when they tap this notification.
        // Easiest is to set a flag in Intent.
        intent.putExtra("OPEN_IMPORTANT", true)
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val summary = items.joinToString("\n") { "⭐ ${it.effectiveScore}: ${it.content.take(30)}..." }
        val title = if (items.size == 1) "Important item needs attention" else "${items.size} important items need attention"
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("Tap to review your high-priority items.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(1001, notification)
    }
}
