package com.vpnsopr.client

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var store: ProfileStore
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentPage = "dashboard"
    private var pendingConfig: String? = null

    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) startVpnNow()
        else showToast("اجازه اتصال VPN داده نشد")
    }

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != VPNSoprVpnService.ACTION_STATE) return
            val state = intent.getStringExtra(VPNSoprVpnService.EXTRA_STATE) ?: "DISCONNECTED"
            val message = intent.getStringExtra(VPNSoprVpnService.EXTRA_MESSAGE).orEmpty()
            getSharedPreferences("vpnsopr_runtime", MODE_PRIVATE).edit()
                .putString("state", state).putString("message", message).apply()
            updateWebState(state, message)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ProfileStore(this)
        window.statusBarColor = android.graphics.Color.rgb(13, 14, 19)
        window.navigationBarColor = android.graphics.Color.rgb(13, 14, 19)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        webView = WebView(this).apply {
            setBackgroundColor(android.graphics.Color.rgb(18, 19, 24))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.mediaPlaybackRequiresUserGesture = true
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.evaluateJavascript("window.VPNSOPR && window.VPNSOPR.onPageReady(); window.vpnState && window.vpnState(${JSONObject.quote(getStatus())}, '');", null)
                }
            }
            addJavascriptInterface(AppBridge(), "Android")
        }
        setContentView(webView)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentPage != "dashboard") navigate("dashboard") else finish()
            }
        })
        webView.loadUrl(pageUrl("dashboard"))

        ContextCompat.registerReceiver(
            this,
            stateReceiver,
            IntentFilter(VPNSoprVpnService.ACTION_STATE),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 710)
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(stateReceiver) }
        worker.shutdownNow()
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }

    private fun pageUrl(key: String): String {
        val page = when (key) {
            "dashboard", "configs", "subscriptions", "settings", "logs", "rules", "rule_add", "apk_portal", "desktop_portal" -> key
            "downloads", "architectures", "integrity-pgp", "engine-specs", "changelog", "github-source" -> "apk_portal"
            else -> "dashboard"
        }
        currentPage = page
        return "file:///android_asset/pages/$page/index.html"
    }

    private fun navigate(key: String) {
        runOnUiThread { webView.loadUrl(pageUrl(key)) }
    }

    private fun connect() {
        runOnUiThread {
            val config = store.selectedConfig()
            if (config.isNullOrBlank()) {
                showToast("اول یک کانفیگ وارد و انتخاب کن")
                navigate("configs")
                return@runOnUiThread
            }
            pendingConfig = config
            val permissionIntent = VpnService.prepare(this)
            if (permissionIntent != null) vpnPermission.launch(permissionIntent) else startVpnNow()
        }
    }

    private fun startVpnNow() {
        val config = pendingConfig ?: store.selectedConfig()
        if (config.isNullOrBlank()) {
            showToast("کانفیگ انتخاب‌شده پیدا نشد")
            return
        }
        val intent = Intent(this, VPNSoprVpnService::class.java)
            .setAction(VPNSoprVpnService.ACTION_START)
            .putExtra(VPNSoprVpnService.EXTRA_CONFIG, config)
        ContextCompat.startForegroundService(this, intent)
        getSharedPreferences("vpnsopr_runtime", MODE_PRIVATE).edit().putString("state", "CONNECTING").apply()
        updateWebState("CONNECTING", "در حال اتصال")
    }

    private fun disconnect() {
        runOnUiThread {
            VPNSoprVpnService.stop(this)
            getSharedPreferences("vpnsopr_runtime", MODE_PRIVATE).edit().putString("state", "DISCONNECTED").apply()
            updateWebState("DISCONNECTED", "اتصال قطع شد")
        }
    }

    private fun updateWebState(state: String, message: String) {
        if (!::webView.isInitialized) return
        val js = "window.vpnState && window.vpnState(${JSONObject.quote(state)}, ${JSONObject.quote(message)});"
        runOnUiThread { webView.evaluateJavascript(js, null) }
    }

    private fun importText(text: String, source: String = "manual") {
        try {
            val parsed = ConfigParser.parseMany(text)
            parsed.forEach { store.add(it.name, it.protocol, it.config, source) }
            notifyImport(true, "${parsed.size} کانفیگ با موفقیت وارد شد")
        } catch (error: Exception) {
            notifyImport(false, error.message ?: "خطا در خواندن کانفیگ")
        }
    }

    private fun notifyImport(ok: Boolean, message: String) {
        val js = "window.VPNSOPR && window.VPNSOPR.profileImportResult(${ok}, ${JSONObject.quote(message)});"
        runOnUiThread {
            webView.evaluateJavascript(js, null)
            if (!ok) Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun addSubscription(input: String) {
        val value = input.trim()
        if (value.isBlank()) {
            notifySubscription(false, "لینک یا کانفیگ را وارد کن")
            return
        }
        if (value.startsWith("https://", true)) {
            store.addSubscriptionUrl(value)
            worker.execute {
                try {
                    val connection = (URL(value).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 20000
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "VPNSOPR-Android/1.0")
                        instanceFollowRedirects = true
                    }
                    val content = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                    connection.disconnect()
                    val parsed = ConfigParser.parseMany(content)
                    parsed.forEach { store.add(it.name, it.protocol, it.config, value) }
                    notifySubscription(true, "${parsed.size} سرور از اشتراک اضافه شد")
                } catch (error: Exception) {
                    notifySubscription(false, error.message ?: "دریافت اشتراک ناموفق بود")
                }
            }
        } else {
            worker.execute {
                try {
                    val parsed = ConfigParser.parseMany(value)
                    parsed.forEach { store.add(it.name, it.protocol, it.config, "subscription-text") }
                    notifySubscription(true, "${parsed.size} کانفیگ وارد شد")
                } catch (error: Exception) {
                    notifySubscription(false, error.message ?: "فرمت اشتراک معتبر نیست")
                }
            }
        }
    }

    private fun notifySubscription(ok: Boolean, message: String) {
        val js = "window.VPNSOPR && window.VPNSOPR.subscriptionResult(${ok}, ${JSONObject.quote(message)});"
        runOnUiThread {
            webView.evaluateJavascript(js, null)
            if (!ok) Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun refreshSubscriptions() {
        val urls = store.subscriptionUrls()
        if (urls.isEmpty()) {
            showToast("هنوز لینک اشتراکی ذخیره نشده است")
            return
        }
        urls.forEach { addSubscription(it) }
    }

    private fun showImportDialog(initial: String = "") {
        val input = EditText(this).apply {
            setText(initial)
            hint = "vless://… یا JSON کانفیگ sing-box"
            minLines = 4
            maxLines = 10
            setPadding(36, 24, 36, 24)
        }
        AlertDialog.Builder(this)
            .setTitle("افزودن کانفیگ VPN")
            .setMessage("لینک اشتراک، لینک سرور یا JSON را وارد کن")
            .setView(input)
            .setNegativeButton("انصراف", null)
            .setPositiveButton("وارد کردن") { _, _ ->
                val value = input.text?.toString().orEmpty()
                worker.execute { importText(value) }
            }
            .show()
    }

    private fun importClipboard() {
        runOnUiThread {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val value = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
            if (value.isBlank()) showToast("متن معتبری در کلیپ‌بورد نیست") else worker.execute { importText(value, "clipboard") }
        }
    }

    private fun showToast(message: String) = runOnUiThread {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getStatus(): String = getSharedPreferences("vpnsopr_runtime", MODE_PRIVATE)
        .getString("state", "DISCONNECTED") ?: "DISCONNECTED"

    inner class AppBridge {
        @JavascriptInterface fun navigate(path: String) = this@MainActivity.navigate(path)
        @JavascriptInterface fun toggleVpn() {
            if (getStatus() == "CONNECTED" || getStatus() == "CONNECTING") disconnect() else connect()
        }
        @JavascriptInterface fun connect() = this@MainActivity.connect()
        @JavascriptInterface fun disconnect() = this@MainActivity.disconnect()
        @JavascriptInterface fun getStatus(): String = this@MainActivity.getStatus()
        @JavascriptInterface fun getProfilesJson(): String = store.publicProfilesJson()
        @JavascriptInterface fun selectProfile(id: String) = store.select(id)
        @JavascriptInterface fun deleteProfile(id: String) {
            val removed = store.remove(id)
            runOnUiThread {
                webView.evaluateJavascript("window.VPNSOPR && window.VPNSOPR.onPageReady();", null)
                showToast(if (removed) "کانفیگ حذف شد" else "کانفیگ پیدا نشد")
            }
        }
        @JavascriptInterface fun promptImport() = runOnUiThread { showImportDialog() }
        @JavascriptInterface fun importClipboard() = this@MainActivity.importClipboard()
        @JavascriptInterface fun addSubscription(url: String) = this@MainActivity.addSubscription(url)
        @JavascriptInterface fun refreshSubscriptions() = this@MainActivity.refreshSubscriptions()
        @JavascriptInterface fun restartVpn() {
            disconnect()
            mainHandler.postDelayed({ connect() }, 900)
        }
        @JavascriptInterface fun showToast(message: String) = this@MainActivity.showToast(message)
        @JavascriptInterface fun copyText(value: String) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("VPNSOPR", value))
            showToast("کپی شد")
        }
    }
}
