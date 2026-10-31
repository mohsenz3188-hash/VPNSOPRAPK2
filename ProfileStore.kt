package com.vpnsopr.client

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("vpnsopr_profiles", Context.MODE_PRIVATE)

    @Synchronized
    fun all(): JSONArray = try {
        JSONArray(prefs.getString(KEY_PROFILES, "[]") ?: "[]")
    } catch (_: Exception) {
        JSONArray()
    }

    @Synchronized
    fun add(name: String, protocol: String, config: String, source: String = "manual"): JSONObject {
        val profiles = all()
        val profile = JSONObject().apply {
            put("id", UUID.randomUUID().toString())
            put("name", name.ifBlank { "$protocol profile" })
            put("protocol", protocol.uppercase())
            put("config", config)
            put("source", source)
            put("createdAt", System.currentTimeMillis())
        }
        profiles.put(profile)
        prefs.edit().putString(KEY_PROFILES, profiles.toString()).apply()
        if (prefs.getString(KEY_SELECTED, null).isNullOrBlank()) {
            prefs.edit().putString(KEY_SELECTED, profile.getString("id")).apply()
        }
        return profile
    }

    @Synchronized
    fun select(id: String) {
        if (all().length() == 0) return
        val found = (0 until all().length()).any { all().optJSONObject(it)?.optString("id") == id }
        if (found) prefs.edit().putString(KEY_SELECTED, id).apply()
    }

    @Synchronized
    fun remove(id: String): Boolean {
        val current = all()
        val kept = JSONArray()
        var removed = false
        for (i in 0 until current.length()) {
            val item = current.optJSONObject(i) ?: continue
            if (item.optString("id") == id) removed = true else kept.put(item)
        }
        if (!removed) return false
        prefs.edit().putString(KEY_PROFILES, kept.toString()).apply()
        if (selectedId() == id) {
            prefs.edit().putString(KEY_SELECTED, kept.optJSONObject(0)?.optString("id")).apply()
        }
        return true
    }

    fun selectedId(): String? = prefs.getString(KEY_SELECTED, null)

    @Synchronized
    fun selected(): JSONObject? {
        val id = selectedId() ?: return null
        val profiles = all()
        for (i in 0 until profiles.length()) {
            val item = profiles.optJSONObject(i) ?: continue
            if (item.optString("id") == id) return item
        }
        return null
    }

    fun selectedConfig(): String? = selected()?.optString("config")?.takeIf { !it.isNullOrBlank() }

    @Synchronized
    fun publicProfilesJson(): String {
        val profiles = all()
        val output = JSONArray()
        val selected = selectedId()
        for (i in 0 until profiles.length()) {
            val item = profiles.optJSONObject(i) ?: continue
            output.put(JSONObject().apply {
                put("id", item.optString("id"))
                put("name", item.optString("name"))
                put("protocol", item.optString("protocol"))
                put("selected", item.optString("id") == selected)
            })
        }
        return output.toString()
    }

    @Synchronized
    fun subscriptionUrls(): List<String> {
        val raw = prefs.getString(KEY_SUBSCRIPTIONS, "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return (0 until arr.length()).mapNotNull { arr.optString(it).takeIf(String::isNotBlank) }
    }

    @Synchronized
    fun addSubscriptionUrl(url: String) {
        val urls = subscriptionUrls().toMutableSet()
        urls.add(url)
        prefs.edit().putString(KEY_SUBSCRIPTIONS, JSONArray(urls.toList()).toString()).apply()
    }

    companion object {
        private const val KEY_PROFILES = "profiles"
        private const val KEY_SELECTED = "selected_profile"
        private const val KEY_SUBSCRIPTIONS = "subscription_urls"
    }
}
