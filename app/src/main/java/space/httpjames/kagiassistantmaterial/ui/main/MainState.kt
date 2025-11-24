package space.httpjames.kagiassistantmaterial.ui.main

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.httpjames.kagiassistantmaterial.AssistantClient
import space.httpjames.kagiassistantmaterial.AssistantThread
import space.httpjames.kagiassistantmaterial.AssistantThreadMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import space.httpjames.kagiassistantmaterial.AssistantThreadMessageRole
import space.httpjames.kagiassistantmaterial.Citation

@Composable
fun rememberMainState(
    assistantClient: AssistantClient,
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): MainState = remember(assistantClient, drawerState, coroutineScope) {
    MainState(assistantClient, drawerState, coroutineScope)
}

class MainState(
    private val assistantClient: AssistantClient,
    val drawerState: DrawerState,
    val coroutineScope: CoroutineScope
) {
    var threads by mutableStateOf<Map<String, List<AssistantThread>>>(emptyMap())
        private set
    var currentThreadId by mutableStateOf<String?>(null)
        private set
    var threadMessages by mutableStateOf<List<AssistantThreadMessage>>(emptyList())

    fun fetchThreads() {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                threads = assistantClient.getThreads()
            }
        }
    }

    fun newChat() {
        println("new chat clicked")
        currentThreadId = null
        threadMessages = emptyList()
        coroutineScope.launch {
            drawerState.close()
        }
    }

    fun onThreadSelected(threadId: String) {
        currentThreadId = threadId
        coroutineScope.launch {
            try {
                assistantClient.fetchStream(
                    streamId = "8ce77b1b-35c5-4262-8821-af3b33d1cf0f",
                    url = "https://kagi.com/assistant/thread_open",
                    method = "POST",
                    body = """{"focus":{"thread_id":"$threadId"}}""",
                    extraHeaders = mapOf("Content-Type" to "application/json"),
                    onChunk = { chunk ->
                        // This will now execute on the main thread
                        if (chunk.header == "messages.json") {
                            val messages = Json.parseToJsonElement(chunk.data)

                            threadMessages = emptyList()

                            for (message in messages.jsonArray) {
                                println(message)
                                val obj = message.jsonObject
                                threadMessages += AssistantThreadMessage(
                                    obj["id"]?.jsonPrimitive?.contentOrNull ?: "",
                                    obj["prompt"]?.jsonPrimitive?.contentOrNull ?: "",
                                    AssistantThreadMessageRole.USER,
                                    emptyList(),
                                )
                                val citations = parseReferencesHtml(obj["references_html"]?.jsonPrimitive?.contentOrNull ?: "")
                                println(citations)
                                threadMessages += AssistantThreadMessage(
                                    (obj["id"]?.jsonPrimitive?.contentOrNull ?: "") + ".reply",
                                    obj["reply"]?.jsonPrimitive?.contentOrNull ?: "",
                                    AssistantThreadMessageRole.ASSISTANT,
                                    citations
                                )
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                println("Error fetching thread: ${e.message}")
                e.printStackTrace()
            }

            drawerState.close()
        }
    }

    fun _setCurrentThreadId(id: String?) {
        currentThreadId = id
    }

    fun openDrawer() {
        coroutineScope.launch {
            drawerState.open()
        }
    }
}

fun parseReferencesHtml(html: String): List<Citation> =
    Jsoup.parse(html)
        .select("ol[data-ref-list] > li > a[href]")
        .map { a -> Citation(url = a.attr("abs:href"), title = a.text()) }



