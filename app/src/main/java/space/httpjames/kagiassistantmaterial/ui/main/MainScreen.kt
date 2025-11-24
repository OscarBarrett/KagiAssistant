package space.httpjames.kagiassistantmaterial.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import space.httpjames.kagiassistantmaterial.AssistantClient
import space.httpjames.kagiassistantmaterial.ui.chat.ChatArea
import space.httpjames.kagiassistantmaterial.ui.message.MessageCenter
import space.httpjames.kagiassistantmaterial.ui.shared.Header

@Composable
fun MainScreen(
    assistantClient: AssistantClient,
    modifier: Modifier = Modifier
) {
    val state = rememberMainState(assistantClient)

    LaunchedEffect(Unit) {
        state.fetchThreads()
    }

    ModalNavigationDrawer(
        drawerState = state.drawerState,
        drawerContent = {
            ThreadsDrawerSheet(
                threads = state.threads,
                onThreadSelected = { state.onThreadSelected(it) }
            )
        }) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = { Header(onMenuClick = { state.openDrawer() }, onNewChatClick = { state.newChat() }) }
        ) { innerPadding ->
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                ChatArea(
                    assistantClient = assistantClient,
                    threadMessages = state.threadMessages,
                    modifier = Modifier
                        .padding(innerPadding)
                        .weight(1f)
                )
                MessageCenter(
                    threadId = state.currentThreadId,
                    assistantClient = assistantClient,
                    threadMessages = state.threadMessages,
                    setThreadMessages = { state.threadMessages = it },
                    coroutineScope = state.coroutineScope,
                    setCurrentThreadId = { it -> state._setCurrentThreadId(it) }
                )
            }
        }
    }
}
