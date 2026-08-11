package com.ichalkaranjirailway.htkich

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ichalkaranjirailway.htkich.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var splashDismissed = false

    companion object {
        private const val SITE_URL = "https://ichalkaranjirailway.github.io/htk-ich/"
        private const val SITE_HOST = "ichalkaranjirailway.github.io"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Native Android 12+ splash screen (no-op / falls back gracefully on older versions)
        installSplashScreen()

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        setupWebView()
        setupSwipeRefresh()
        setupBackHandling()

        binding.retryButton.setOnClickListener { loadSite() }

        loadSite()
    }

    private fun setupWebView() {
        val webView = binding.webView
        val settings: WebSettings = webView.settings

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.mediaPlaybackRequiresUserGesture = true

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url
                return handleUrl(url)
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                binding.swipeRefresh.isRefreshing = false
                hideOfflineLayout()
                dismissSplash()
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                // Only treat main-frame failures as a full offline/error screen
                if (request.isForMainFrame) {
                    binding.swipeRefresh.isRefreshing = false
                    showOfflineLayout()
                    dismissSplash()
                }
            }
        }

        webView.webChromeClient = WebChromeClient()

        // Handle real file downloads (non-PDF attachments) via the system Download Manager
        webView.setDownloadListener { url, _, contentDisposition, mimeType, _ ->
            try {
                val request = DownloadManager.Request(Uri.parse(url))
                request.setMimeType(mimeType)
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                val fileName = Uri.parse(url).lastPathSegment ?: "download"
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Toast.makeText(this, getString(R.string.downloading_file), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                openInExternalBrowser(Uri.parse(url))
            }
        }
    }

    /**
     * Central place that decides what happens with every link the site tries to load.
     * Returns true if we intercepted it (i.e. WebView should NOT load it itself).
     */
    private fun handleUrl(url: Uri): Boolean {
        val scheme = url.scheme?.lowercase()

        return when {
            // Phone numbers
            scheme == "tel" -> {
                startExternalIntent(Intent(Intent.ACTION_DIAL, url))
                true
            }
            // Email links
            scheme == "mailto" -> {
                startExternalIntent(Intent(Intent.ACTION_SENDTO, url))
                true
            }
            // WhatsApp / other app deep links (whatsapp://, intent://, market://, etc.)
            scheme != "http" && scheme != "https" -> {
                startExternalIntent(Intent(Intent.ACTION_VIEW, url))
                true
            }
            // Our own site: keep it inside the app
            url.host?.contains(SITE_HOST) == true -> {
                false
            }
            // PDFs / documents hosted elsewhere (e.g. Google Drive): hand off to the
            // browser or a document viewer, since Android WebView can't render these itself.
            isDocumentLink(url) -> {
                openInExternalBrowser(url)
                true
            }
            // Any other external domain (govt sites, YouTube, social media, Change.org, etc.)
            else -> {
                openInExternalBrowser(url)
                true
            }
        }
    }

    private fun isDocumentLink(url: Uri): Boolean {
        val path = url.path?.lowercase() ?: return false
        return path.endsWith(".pdf") || path.endsWith(".doc") || path.endsWith(".docx") ||
            path.endsWith(".xls") || path.endsWith(".xlsx") || path.endsWith(".ppt") || path.endsWith(".pptx")
    }

    private fun openInExternalBrowser(url: Uri) {
        startExternalIntent(Intent(Intent.ACTION_VIEW, url))
    }

    private fun startExternalIntent(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.no_app_found), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.railway_blue)
        binding.swipeRefresh.setOnRefreshListener { loadSite() }
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    showExitConfirmation()
                }
            }
        })
    }

    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.exit_confirm_title))
            .setPositiveButton(getString(R.string.exit_confirm_yes)) { _, _ -> finish() }
            .setNegativeButton(getString(R.string.exit_confirm_no), null)
            .show()
    }

    private fun loadSite() {
        if (isNetworkAvailable()) {
            hideOfflineLayout()
            binding.webView.loadUrl(SITE_URL)
        } else {
            binding.swipeRefresh.isRefreshing = false
            showOfflineLayout()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showOfflineLayout() {
        binding.offlineLayout.visibility = View.VISIBLE
    }

    private fun hideOfflineLayout() {
        binding.offlineLayout.visibility = View.GONE
    }

    /**
     * Fades out the custom splash overlay once the site has (successfully or not)
     * finished its first load attempt. This is the fallback splash shown on Android
     * versions below 12, which don't have the system SplashScreen API.
     */
    private fun dismissSplash() {
        if (splashDismissed) return
        splashDismissed = true
        binding.splashLayout.animate()
            .alpha(0f)
            .setDuration(250)
            .withEndAction { binding.splashLayout.visibility = View.GONE }
            .start()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> {
                shareSite()
                true
            }
            R.id.action_reload -> {
                loadSite()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareSite() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, binding.webView.url ?: SITE_URL)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_chooser_title)))
    }
}
