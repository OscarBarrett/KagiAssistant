package space.httpjames.kagiassistantmaterial.ui.message

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import space.httpjames.kagiassistantmaterial.AssistantClient
import space.httpjames.kagiassistantmaterial.AssistantThreadMessage
import space.httpjames.kagiassistantmaterial.AssistantThreadMessageRole
import space.httpjames.kagiassistantmaterial.Citation
import space.httpjames.kagiassistantmaterial.KagiPromptRequest
import space.httpjames.kagiassistantmaterial.KagiPromptRequestFocus
import space.httpjames.kagiassistantmaterial.KagiPromptRequestProfile
import space.httpjames.kagiassistantmaterial.KagiPromptRequestThreads
import space.httpjames.kagiassistantmaterial.ui.main.parseReferencesHtml
import java.util.UUID

@Composable
fun rememberMessageCenterState(
    assistantClient: AssistantClient,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    threadMessages: List<AssistantThreadMessage> = emptyList(),
    setThreadMessages: (List<AssistantThreadMessage>) -> Unit,
    setCurrentThreadId: (String?) -> Unit,
): MessageCenterState {
    val haptics = LocalHapticFeedback.current
    val prefs = LocalContext.current.getSharedPreferences("assistant_prefs", Context.MODE_PRIVATE)

    val currentThreadMessages = rememberUpdatedState(threadMessages)
    val currentSetThreadMessages = rememberUpdatedState(setThreadMessages)

    return remember(assistantClient, coroutineScope, prefs) {
        MessageCenterState(
            haptics,
            prefs,
            assistantClient,
            coroutineScope,
            { currentThreadMessages.value },
            { currentSetThreadMessages.value(it) },
            setCurrentThreadId
        )
    }
}

class MessageCenterState(
    private val haptics: HapticFeedback,
    private val prefs: SharedPreferences,
    private val assistantClient: AssistantClient,
    private val coroutineScope: CoroutineScope,
    private val getThreadMessages: () -> List<AssistantThreadMessage>,
    private val setThreadMessages: (List<AssistantThreadMessage>) -> Unit,
    private val setCurrentThreadId: (String?) -> Unit
) {
    var text by mutableStateOf("")
        private set

    var isSearchEnabled by mutableStateOf(false)
        private set

    var inProgressAssistantMessageId by mutableStateOf<String?>(null)
        private set

    var showModelBottomSheet by mutableStateOf(false)
        private set

    var profiles by mutableStateOf<List<AssistantProfile>>(emptyList())
        private set

    var showAttachmentBottomSheet by mutableStateOf(false)
        private set


    init {
        fetchProfiles()
    }

    fun onDismissAttachmentBottomSheet() {
        showAttachmentBottomSheet = false
    }

    fun showAttachmentBottomSheet() {
        showAttachmentBottomSheet = true
    }


    private fun fetchProfiles() {
        coroutineScope.launch {
            val streamId = UUID.randomUUID().toString()
            assistantClient.fetchStream(
                streamId = streamId,
                url = "https://kagi.com/assistant/profile_list",
                method = "POST",
                body = """{}""",
                extraHeaders = mapOf("Content-Type" to "application/json"),
                onChunk = { chunk ->
                    if (chunk.header == "profiles.json") {
                        val json = Json.parseToJsonElement(chunk.data)
                        val nest = json.jsonObject
                        val profiles = nest["profiles"]?.jsonArray ?: emptyList()

                        this@MessageCenterState.profiles = emptyList()

                        for (profile in profiles) {
                            val obj = profile.jsonObject
                            this@MessageCenterState.profiles += AssistantProfile(
                                obj["model"]?.jsonPrimitive?.contentOrNull ?: "",
                                obj["model_provider"]?.jsonPrimitive?.contentOrNull ?: "",
                                obj["name"]?.jsonPrimitive?.contentOrNull ?: "",
                                obj["model_input_limit"]?.jsonPrimitive?.int ?: 40000,
                            )
                        }
                    }
                }
            )
        }
    }


    fun setThreadMessages(transform: (List<AssistantThreadMessage>) -> List<AssistantThreadMessage>) {
        setThreadMessages(transform(getThreadMessages()))
    }

    fun onTextChanged(newText: String) {
        text = newText
    }

    fun toggleSearch() {
        isSearchEnabled = !isSearchEnabled
    }

    fun openModelBottomSheet() {
        showModelBottomSheet = true
    }

    fun dismissModelBottomSheet() {
        showModelBottomSheet = false
    }

    fun getProfile(): AssistantProfile? {
        val id = prefs.getString("profile", null) ?: return null

        return profiles.find { it.id == id }
    }

    fun sendMessage(threadId: String?) {
        var messageId = UUID.randomUUID().toString()
        inProgressAssistantMessageId = messageId

        // Local copy for stream processing
        var localMessages = getThreadMessages()

        setThreadMessages({ current ->
            localMessages = current + AssistantThreadMessage(
                id = messageId,
                content = text,
                role = AssistantThreadMessageRole.USER,
            )
            localMessages
        })


        coroutineScope.launch {
            val streamId = UUID.randomUUID().toString()

            val latestAssistantMessageId = getThreadMessages().lastOrNull { it.role == AssistantThreadMessageRole.ASSISTANT }?.id

            val focus = KagiPromptRequestFocus(
                threadId,
                latestAssistantMessageId,
                text,
                "00000000-0000-4000-0000-000000000000",
            )

            text = ""

            val requestBody = KagiPromptRequest(
                focus,
                KagiPromptRequestProfile(
                    null,
                    isSearchEnabled,
                    null,
                    getProfile()?.id ?: "",
                    false,
                ),
                // todo: regeneration edit logic requires undefined (which actually means omission)
                listOf(
                    KagiPromptRequestThreads(
                        listOf(),
                        true,
                        false
                    )
                )
            )

            val url = "https://kagi.com/assistant/prompt" // todo: needs message_regenerate handling

            val moshi = Moshi.Builder()
                .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                .build()

            val jsonAdapter = moshi.adapter(KagiPromptRequest::class.java)
            val jsonString = jsonAdapter.toJson(requestBody)

            assistantClient.fetchStream(
                streamId = streamId,
                url = url,
                method = "POST",
                body = jsonString,
                extraHeaders = mapOf("Content-Type" to "application/json"),
                onChunk = { chunk ->
                    println("CHUNK RECEIVED: $chunk")
                    if (chunk.header == "thread.json") {
                        // get the id and call the setter
                        val json = Json.parseToJsonElement(chunk.data)
                        val obj = json.jsonObject
                        val id = obj["id"]?.jsonPrimitive?.contentOrNull
                        setCurrentThreadId(id)
                    }

                    if (chunk.header == "new_message.json") {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        val json = Json.parseToJsonElement(chunk.data)
                        val obj = json.jsonObject
                        val text = obj["reply"]?.jsonPrimitive?.contentOrNull ?: ""
                        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: ""
                        val citations = obj["references_html"]?.jsonPrimitive?.contentOrNull ?: ""

                        println("ADDING NEW ID: $id")
                        inProgressAssistantMessageId = id
                        messageId = id

                        // Update local state first
                        if (localMessages.find { it.id == id } != null) {
                            var preparedCitations: List<Citation> = emptyList()
                            if (citations.isNotBlank()) {
                                preparedCitations = parseReferencesHtml(obj["references_html"]?.jsonPrimitive?.contentOrNull ?: "")
                            }
                            localMessages = localMessages.map {
                                if (it.id == id) it.copy(content = text, citations = preparedCitations) else it
                            }
                        } else {
                            localMessages = localMessages + AssistantThreadMessage(
                                id = id,
                                content = text,
                                role = AssistantThreadMessageRole.ASSISTANT,
                            )
                        }
                        // Then sync to parent
                        setThreadMessages { localMessages }
                    }

                    if (chunk.header == "tokens.json") {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val json = Json.parseToJsonElement(chunk.data)
                        val obj = json.jsonObject
                        val text = obj["text"]?.jsonPrimitive?.contentOrNull ?: ""
                        val incomingId = obj["id"]?.jsonPrimitive?.contentOrNull ?: ""

                        // Use local state, not getThreadMessages()
                        localMessages = localMessages.map {
                            if (it.id == incomingId)
                                it.copy(content = text)
                            else
                                it
                        }
                        // Sync to parent
                        setThreadMessages { localMessages }
                    }
                }
            )

        }

//        simulation:
//        coroutineScope.launch {
//            val finalMessage = "<p>hi</p><h1>Hello, I am an AI assistant. How can I help?</h1>"
//            for (i in finalMessage.indices) {
//                println("doing update")
//                delay(100)
//                setThreadMessages { current ->
//                    current.map { msg ->
//                        if (msg.id == messageId)
//                            msg.copy(content = finalMessage.take(i + 1))
//                        else
//                            msg
//                    }
//                }
//            }
//        }
    }

}