package space.httpjames.kagiassistantmaterial.ui.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import space.httpjames.kagiassistantmaterial.R
import space.httpjames.kagiassistantmaterial.ui.chat.HtmlCard
import space.httpjames.kagiassistantmaterial.ui.chat.HtmlPreprocessor
import space.httpjames.kagiassistantmaterial.ui.message.ShimmeringMessagePlaceholder

@Composable
fun AssistantMessageArea(
    state: AssistantOverlayState,
    modifier: Modifier = Modifier
) {
    if (state.assistantMessage.isNotEmpty() || state.isWaitingForMessageFirstToken || !state.assistantDone) {
        Column(modifier = modifier) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 12.dp,
                        start = 12.dp,
                        end = 12.dp,
                        bottom = 0.dp
                    )
            ) {
                Icon(
                    painter = painterResource(R.drawable.fetch_ball_icon),
                    contentDescription = "",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(32.dp),
                )

                FilledIconButton(
                    onClick = {
                        if (state.isSpeaking) {
                            state.stopSpeaking()
                        } else {
                            state.restartSpeaking()
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = if (state.isSpeaking) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isSpeaking) "Stop speaking" else "Restart speaking",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (state.assistantMessage.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 60.dp)
                        .fillMaxWidth()
                ) {
                    HtmlCard(
                        html = HtmlPreprocessor.preprocess("<p>${state.assistantMessage}</p>"),
                        onHeightMeasured = {},
                        minHeight = 60.dp
                    )
                }
            } else if (state.isWaitingForMessageFirstToken) {
                ShimmeringMessagePlaceholder(
                    showNum = 2
                )
            }
        }
    }
}
