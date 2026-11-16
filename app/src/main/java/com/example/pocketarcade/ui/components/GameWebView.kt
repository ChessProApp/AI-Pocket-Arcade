package com.example.pocketarcade.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

private class ArcadeJsBridge(
    private val onScoreUpdateCallback: (Int) -> Unit
) {
    @JavascriptInterface
    fun onScoreUpdate(score: Int) {
        onScoreUpdateCallback(score)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GameWebView(
    htmlContent: String,
    onScoreUpdate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                loadWithOverviewMode = true
                useWideViewPort = true
                cacheMode = WebSettings.LOAD_NO_CACHE
            }
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            addJavascriptInterface(ArcadeJsBridge(onScoreUpdate), "AndroidBridge")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.stopLoading()
            webView.removeJavascriptInterface("AndroidBridge")
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        update = { view ->
            if (view.tag != htmlContent.hashCode()) {
                view.tag = htmlContent.hashCode()
                view.loadDataWithBaseURL(
                    "[https://pocketarcade.local/](https://pocketarcade.local/)",
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        modifier = modifier
    )
}
