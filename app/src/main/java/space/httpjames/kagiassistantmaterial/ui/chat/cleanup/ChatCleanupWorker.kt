package space.httpjames.kagiassistantmaterial.ui.chat.cleanup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import space.httpjames.kagiassistantmaterial.AssistantClient

class ChatCleanupWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val chatId = inputData.getString("CHAT_ID") ?: return Result.failure()
        val sessionToken = inputData.getString("SESSION_TOKEN") ?: return Result.failure()

        return try {
            val client = AssistantClient(sessionToken)
            client.deleteChat(chatId)
            println("Chat $chatId deleted in the background")
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
