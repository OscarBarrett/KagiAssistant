package space.httpjames.kagiassistantmaterial.ui.chat.cleanup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
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

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // this is obviously a network request so no use to do it offline
            .build()

        // the exponential backoff will work here if we go offline
        val workRequest = OneTimeWorkRequestBuilder<ChatCleanupWorker>()
            .setInitialDelay(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "cleanup_$chatId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
