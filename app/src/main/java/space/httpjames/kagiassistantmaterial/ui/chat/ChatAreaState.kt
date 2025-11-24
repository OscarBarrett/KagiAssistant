package space.httpjames.kagiassistantmaterial.ui.chat

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import space.httpjames.kagiassistantmaterial.AssistantClient

@Composable
fun rememberChatAreaState(
    assistantClient: AssistantClient,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    lazyListState: LazyListState = rememberLazyListState()
): ChatAreaState = remember(assistantClient, coroutineScope, lazyListState) {
    ChatAreaState(assistantClient, coroutineScope, lazyListState)
}

class ChatAreaState(
    private val assistantClient: AssistantClient,
    private val coroutineScope: CoroutineScope,
    val lazyListState: LazyListState
) {

}