package com.example.sgrf

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

private const val SITE_URL = "https://sgrf.eloenaypereira.com.br/"

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        val statusBarBg = findViewById<View>(R.id.statusBarBg)
        val navBarBg = findViewById<View>(R.id.navBarBg)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        webView = findViewById(R.id.webView)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            statusBarBg.layoutParams.height = systemBars.top
            statusBarBg.requestLayout()
            navBarBg.layoutParams.height = systemBars.bottom
            navBarBg.requestLayout()
            insets
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                progressBar.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    progressBar.visibility = View.GONE
                }
            }
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            useWideViewPort = true
            textZoom = 100
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
            flush()
        }

        webView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            val cookies = CookieManager.getInstance().getCookie(url) ?: ""
            val filename = extractFilename(contentDisposition) ?: "resumo-financeiro.pdf"

            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                addRequestHeader("Cookie", cookies)
                addRequestHeader("User-Agent", userAgent)
                setTitle(filename)
                setDescription("Baixando PDF...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            }

            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(this, "Baixando $filename...", Toast.LENGTH_SHORT).show()
        })

        webView.loadUrl(SITE_URL)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack()
                else finish()
            }
        })
    }

    private fun extractFilename(contentDisposition: String?): String? {
        if (contentDisposition.isNullOrBlank()) return null
        // filename*=UTF-8''nome-do-arquivo.pdf
        val utf8 = Regex("""filename\*=UTF-8''(.+)""", RegexOption.IGNORE_CASE).find(contentDisposition)
        if (utf8 != null) return Uri.decode(utf8.groupValues[1].trim())
        // filename="nome-do-arquivo.pdf"
        val plain = Regex("""filename="?([^";]+)"?""", RegexOption.IGNORE_CASE).find(contentDisposition)
        return plain?.groupValues?.get(1)?.trim()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            CookieManager.getInstance().removeSessionCookies(null)
            CookieManager.getInstance().flush()
        }
    }
}
