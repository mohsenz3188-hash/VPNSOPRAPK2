package com.vpnsopr.client

import android.app.Application
import android.os.Build
import android.util.Log
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.SetupOptions
import java.util.Locale

class VPNSoprApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching {
            Libbox.setLocale(Locale.getDefault().toLanguageTag())
            val options = SetupOptions().apply {
                basePath = filesDir.absolutePath
                workingPath = (getExternalFilesDir(null) ?: filesDir).absolutePath
                tempPath = cacheDir.absolutePath
                logMaxLines = 2000
                debug = false
                appVersion = BuildConfig.VERSION_CODE.toString()
                appMarketingVersion = BuildConfig.VERSION_NAME
            }
            Libbox.setup(options)
        }.onFailure {
            Log.e("VPNSOPR", "sing-box initialization failed", it)
        }
    }
}
