package space.httpjames.kagiassistantmaterial.ui.landing

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.httpjames.kagiassistantmaterial.R

@Preview
@Composable
fun LandingScreen(onLoginSuccess: (String) -> Unit = {}) {
    val state = rememberLandingScreenState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var waitingOnAuth: Boolean by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.padding(top = 90.dp, start = 24.dp)
            ) {
                Text(
                    "Kagi",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Assistant",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            BouncingBall()

            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 30.dp, start = 24.dp, end = 24.dp)
            ) {
                Button(
                    enabled = !waitingOnAuth,
                    onClick = {
                        coroutineScope.launch {
                            val data = state.startCeremony()
                            waitingOnAuth = true
                            if (data.isSuccess) {
                                val token = data.getOrNull()
                                if (token != null) {
                                    val url = "https://kagi.com/settings/qr_authorize?t=$token"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                ) {
                    if (waitingOnAuth) {
                        CircularProgressIndicator()
                    } else {
                        Text("Sign in", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("We'll sync all of your chats to this device", style = MaterialTheme.typography.labelMedium, color = Color.Gray.copy(alpha = 0.8f))
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)          // wait 1 s
            val token = state.checkCeremony()
            if (token != null) {
                onLoginSuccess(token)
                break
            }
        }

    }
}

@Composable
fun BouncingBall() {
    val haptics = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition("bouncing_ball")

// 1. The Vertical Bounce (Gravity)
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -150f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    // Observe changes to offsetY to trigger haptics on impact
    LaunchedEffect(Unit) {
        var isTouchingFloor = true // Initialize true to skip haptics on initial render
        snapshotFlow { offsetY }
            .collect { y ->
                // Check if the ball is close to the bottom (0f)
                // We use a threshold (e.g., -10f) because the animation frames might not hit exactly 0f
                if (y > -10f) {
                    if (!isTouchingFloor) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isTouchingFloor = true
                    }
                } else {
                    // Reset state when ball moves up
                    isTouchingFloor = false
                }
            }
    }

// 2. The Rotation (Syncs with gravity to "hang" at the top)
    val rotation by infiniteTransition.animateFloat(
        initialValue = -15f, // Tilt left at bottom
        targetValue = 15f,   // Tilt right at top (slowly)
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Icon(
            painter = painterResource(id = R.drawable.fetch_ball_icon),
            contentDescription = null,
            modifier = Modifier
                .offset(y = offsetY.dp) // Apply offset first (position)
                .rotate(rotation)       // Apply rotation second (spin around center)
                .size(300.dp),
            tint = Color.Unspecified
        )
        HorizontalDivider()
    }
}
