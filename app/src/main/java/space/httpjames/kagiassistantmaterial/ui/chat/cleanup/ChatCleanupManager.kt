package space.httpjames.kagiassistantmaterial.ui.chat.cleanup

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object ChatCleanupManager {
    fun schedule(context: Context, chatId: String, sessionToken: String) {
        val data = workDataOf(
            "CHAT_ID" to chatId,
            "SESSION_TOKEN" to sessionToken
        )

        val workRequest = OneTimeWorkRequestBuilder<ChatCleanupWorker>()
            .setInitialDelay(15, TimeUnit.MINUTES)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "cleanup_$chatId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
