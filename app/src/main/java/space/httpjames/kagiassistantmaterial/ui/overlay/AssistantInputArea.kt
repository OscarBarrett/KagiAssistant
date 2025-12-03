package space.httpjames.kagiassistantmaterial.ui.overlay

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AssistantInputArea(
    state: AssistantOverlayState,
    lines: Int,
    onLinesChanged: (Int) -> Unit,
    borderAlpha: Float,
    modifier: Modifier = Modifier
) {
    val localFocusContext = LocalFocusManager.current
    val col = MaterialTheme.colorScheme.primary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 60.dp)
            .background(
                MaterialTheme.colorScheme.surfaceContainerHighest,
                RoundedCornerShape(
                    16.dp
                )
            )
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = if (lines > 1) Alignment.Bottom else Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            val focusRequester = remember { FocusRequester() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { focusRequester.requestFocus() }
            ) {
                BasicTextField(
                    value = state.text,
                    onValueChange = { state.onTextChanged(it) },
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    onTextLayout = { textLayoutResult ->
                        onLinesChanged(textLayoutResult.lineCount)
                    },
                    maxLines = Int.MAX_VALUE,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.None
                    ),
                    modifier = Modifier
                        .background(Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .onFocusEvent { event ->
                            if (event.isFocused) {
                                state.stopListening()
                            }
                            state._setIsTypingMode(event.isFocused)
                        }
                        .animateContentSize()
                        .fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (state.text.isEmpty()) {
                                Text(
                                    text = "Speak or tap to type",
                                    style = LocalTextStyle.current.copy(
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = .5f
                                        )
                                    )
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }

            FilledIconButton(
                onClick = {
                    if (state.isListening) {
                        state.stopListening()
                    } else if (!state.isListening && state.text.isEmpty()) {
                        state.restartFlow()
                    } else {
                        state.sendMessage()
                        localFocusContext.clearFocus()
                        state.onTextChanged("")
                    }
                },
                modifier = Modifier
                    .border(
                        width = 4.dp,
                        if (state.isListening) col.copy(alpha = borderAlpha)
                        else Color.Transparent,
                        CircleShape
                    )
                    .padding(8.dp)
                    .size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (state.isListening) Color.Transparent else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (state.isListening) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = if (state.isTypingMode) Icons.Default.Send else Icons.Default.Mic,
                    contentDescription = if (state.isTypingMode) "Send message" else null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
