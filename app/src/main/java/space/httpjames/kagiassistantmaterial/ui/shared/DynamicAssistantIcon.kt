package space.httpjames.kagiassistantmaterial.ui.shared

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import space.httpjames.kagiassistantmaterial.R

@Composable
fun DynamicAssistantIcon(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.fetch_ball_icon),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = modifier
    )
}