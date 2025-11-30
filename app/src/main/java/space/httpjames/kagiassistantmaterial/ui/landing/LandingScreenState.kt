package space.httpjames.kagiassistantmaterial.ui.landing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.httpjames.kagiassistantmaterial.AssistantClient
import space.httpjames.kagiassistantmaterial.QrRemoteSessionDetails

@Composable
fun rememberLandingScreenState(
    assistantClient: AssistantClient = AssistantClient("null"),
): LandingScreenState {

    return remember(assistantClient) {
        LandingScreenState(assistantClient)
    }
}

class LandingScreenState(
    private val assistantClient: AssistantClient,
) {
    var authSessionDetails by mutableStateOf<QrRemoteSessionDetails?>(null)
        private set

    suspend fun startCeremony(): Result<String> = withContext(Dispatchers.IO) {
        val qrRemoteSession = assistantClient.getQrRemoteSession()

        if (qrRemoteSession.isSuccess) {
            authSessionDetails = qrRemoteSession.getOrNull()
            Result.success(authSessionDetails!!.token)
        } else {
            Result.failure(qrRemoteSession.exceptionOrNull() ?: Exception("Unknown error"))
        }
    }

    suspend fun checkCeremony(): String? {
        if (authSessionDetails == null) return null

        return withContext(Dispatchers.IO) {
            val check = assistantClient.checkQrRemoteSession(authSessionDetails!!)
            if (check.isSuccess) {
                val token = check.getOrNull()
                if (token != null) {
                    authSessionDetails = null
                    return@withContext token
                }
            }

            return@withContext null
        }
    }
}
