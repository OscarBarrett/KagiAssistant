package space.httpjames.kagiassistantmaterial.ui.chat

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material3.placeholder
import com.google.accompanist.placeholder.material3.shimmer
import space.httpjames.kagiassistantmaterial.AssistantThreadMessageRole
import space.httpjames.kagiassistantmaterial.Citation
import space.httpjames.kagiassistantmaterial.R
import java.net.URI


@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ChatMessage(
    id: String,
    content: String,
    role: AssistantThreadMessageRole,
    citations: List<Citation> = emptyList(),
) {
    val isMe = role == AssistantThreadMessageRole.USER
    val background = if (isMe) MaterialTheme.colorScheme.primary
    else Color.Transparent
    val shape = if (isMe)
        RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
    else
        RoundedCornerShape(0.dp)

    var showSourcesSheet by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Surface(
            modifier = Modifier
                .align(if (isMe) Alignment.CenterEnd else Alignment.CenterStart)
                .then(if (isMe) Modifier.widthIn(max = maxWidth * 0.75f) else Modifier),
            shape = shape,
            color = background,
            tonalElevation = 2.dp
        ) {
            if (isMe) {
                Text(
                    text = content,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column {
                    Icon(
                        painter = painterResource(R.drawable.fetch_ball_icon),
                        contentDescription = "",
                        tint = Color.Unspecified,
                        modifier = Modifier.padding(12.dp).size(32.dp),
                    )

                    if (content.isEmpty()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp)
                                    .placeholder(
                                        visible = true,
                                        highlight = PlaceholderHighlight.shimmer(),
                                    )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(20.dp)
                                    .placeholder(
                                        visible = true,
                                        highlight = PlaceholderHighlight.shimmer(),
                                    )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .height(20.dp)
                                    .placeholder(
                                        visible = true,
                                        highlight = PlaceholderHighlight.shimmer(),
                                    )
                            )
                        }
                    } else {
                        HtmlCard(html = HtmlPreprocessor.preprocess(content), key = id, )
                    }

                    if (citations.isNotEmpty()) {
                        SourcesButton(
                            domains = citations.take(3).map { URI(it.url).host?: "" },
                            text = "Sources",
                            onClick = {
                                showSourcesSheet = true
                            }
                        )
                    }
                }
            }


        }
    }

    if (showSourcesSheet) {
        SourcesBottomSheet(citations = citations, onDismissRequest = {
            showSourcesSheet = false
        })
    }

}