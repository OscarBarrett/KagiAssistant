package space.httpjames.kagiassistantmaterial.ui.settings

import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController

private const val TARGET_URL = "https://kagi.com/settings/assistant"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantSettingsWebViewScreen(
    sessionToken: String,
    navController: NavController,
) {
    // State to track if WebView can go back, hold WebView reference, and loading state
    var canGoBack by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Back handler that goes back in WebView or navigates back
    BackHandler(enabled = true) {
        when {
            canGoBack -> webView?.goBack()
            else -> navController.popBackStack()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Assistant Settings",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            canGoBack -> webView?.goBack()
                            else -> navController.popBackStack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        // Enable JavaScript
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true

                        // Set up WebViewClient to handle page navigation
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                canGoBack = canGoBack()

                                // Remove the first <header> element
                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                        const header = document.querySelector('header');
                                        if (header) {
                                            header.remove();
                                        }
                                        return "done";
                                    })();
                                    """.trimIndent()
                                ) { result ->
                                    // Set loading to false after header is removed
                                    // The callback receives the JS return value async
                                    isLoading = false
                                }
                            }
                        }

                        // Set up WebChromeClient for progress and title
                        webChromeClient = WebChromeClient()

                        // Inject the session cookie
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                        cookieManager.setCookie(
                            TARGET_URL,
                            "kagi_session=$sessionToken"
                        )
                        cookieManager.setCookie(
                            "https://kagi.com",
                            "kagi_session=$sessionToken"
                        )

                        // Flush the cookies to ensure they're set
                        cookieManager.flush()

                        // Store WebView reference
                        webView = this

                        // Load the URL
                        loadUrl(TARGET_URL)
                    }
                },
                update = { view ->
                    webView = view
                    canGoBack = view.canGoBack()
                    // Hide WebView while loading, show when ready
                    view.alpha = if (isLoading) 0f else 1f
                }
            )
        }
    }
}
