package com.vpnsopr.client

import android.util.Base64 as AndroidBase64
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object ConfigParser {
    data class Parsed(val name: String, val protocol: String, val config: String)

    fun parseMany(input: String): List<Parsed> {
        val original = input.trim().removePrefix("\uFEFF")
        require(original.isNotBlank()) { "کانفیگ خالی است" }

        val jsonText = decodeSubscription(original)
        if (jsonText.trimStart().startsWith("{")) {
            return listOf(parseSingBoxJson(jsonText))
        }
        if (jsonText.trimStart().startsWith("[Interface]", true)) {
            return listOf(parseWireGuardIni(jsonText))
        }

        val lines = jsonText.lines().map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("//") }
        val candidates = if (lines.size <= 1 && jsonText.contains(" ")) {
            jsonText.split(Regex("\\s+")).filter { it.contains("://") }
        } else lines
        val parsed = candidates.mapNotNull { line -> runCatching { parseLink(line) }.getOrNull() }
        require(parsed.isNotEmpty()) { "فرمت کانفیگ پشتیبانی نمی‌شود. لینک یا JSON معتبر sing-box وارد کن." }
        return parsed
    }

    fun parseLink(raw: String): Parsed {
        val link = raw.trim()
        return when {
            link.startsWith("vless://", true) -> parseVless(link)
            link.startsWith("vmess://", true) -> parseVmess(link)
            link.startsWith("trojan://", true) -> parseTrojan(link)
            link.startsWith("ss://", true) || link.startsWith("shadowsocks://", true) -> parseShadowsocks(link)
            link.startsWith("hysteria2://", true) || link.startsWith("hy2://", true) -> parseHysteria2(link)
            link.startsWith("tuic://", true) -> parseTuic(link)
            link.startsWith("{") -> parseSingBoxJson(link)
            else -> throw IllegalArgumentException("پروتکل این لینک پشتیبانی نمی‌شود")
        }
    }

    private fun parseVless(link: String): Parsed {
        val uri = URI(link)
        val uuid = uri.userInfo?.substringBefore(':')?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("UUID در لینک VLESS وجود ندارد")
        val host = uri.host ?: throw IllegalArgumentException("آدرس سرور VLESS نامعتبر است")
        val port = uri.port.takeIf { it > 0 } ?: 443
        val q = query(uri.rawQuery)
        val security = q["security"].orEmpty().lowercase()
        val outbound = JSONObject().apply {
            put("type", "vless")
            put("tag", "proxy")
            put("server", host)
            put("server_port", port)
            put("uuid", uuid)
            q["flow"]?.takeIf { it.isNotBlank() }?.let { put("flow", it) }
            if (security == "tls" || security == "reality") {
                put("tls", tlsObject(host, q, security == "reality"))
            }
            transportObject(q)?.let { put("transport", it) }
        }
        return Parsed(nameFrom(uri, "VLESS - $host"), "VLESS", fullConfig(outbound))
    }

    private fun parseVmess(link: String): Parsed {
        val payload = link.removePrefix("vmess://").substringBefore('#')
        val decoded = decodeBase64(payload)
        val data = JSONObject(decoded)
        val host = data.optString("add").takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("آدرس سرور VMess وجود ندارد")
        val port = data.optInt("port", 443)
        val uuid = data.optString("id").takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("UUID در لینک VMess وجود ندارد")
        val network = data.optString("net", "tcp").lowercase()
        val outbound = JSONObject().apply {
            put("type", "vmess")
            put("tag", "proxy")
            put("server", host)
            put("server_port", port)
            put("uuid", uuid)
            put("security", data.optString("scy", "auto").ifBlank { "auto" })
            put("alter_id", data.optInt("aid", 0))
            if (data.optString("tls").equals("tls", true)) {
                put("tls", JSONObject().put("enabled", true)
                    .put("server_name", data.optString("sni").ifBlank { data.optString("host").ifBlank { host } })
                    .put("utls", JSONObject().put("enabled", true).put("fingerprint", "chrome")))
            }
            when (network) {
                "ws" -> put("transport", JSONObject().put("type", "ws")
                    .put("path", data.optString("path", "/"))
                    .put("headers", JSONObject().put("Host", data.optString("host", ""))))
                "grpc" -> put("transport", JSONObject().put("type", "grpc").put("service_name", data.optString("path", "")))
                "http", "h2" -> put("transport", JSONObject().put("type", "http")
                    .put("host", JSONArray().put(data.optString("host", host)))
                    .put("path", data.optString("path", "/")))
            }
        }
        return Parsed(data.optString("ps").ifBlank { "VMess - $host" }, "VMESS", fullConfig(outbound))
    }

    private fun parseTrojan(link: String): Parsed {
        val uri = URI(link)
        val password = uri.userInfo?.substringBefore(':')?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("رمز Trojan وجود ندارد")
        val host = uri.host ?: throw IllegalArgumentException("آدرس سرور Trojan نامعتبر است")
        val port = uri.port.takeIf { it > 0 } ?: 443
        val q = query(uri.rawQuery)
        val outbound = JSONObject().apply {
            put("type", "trojan")
            put("tag", "proxy")
            put("server", host)
            put("server_port", port)
            put("password", password)
            put("tls", tlsObject(host, q, false))
            transportObject(q)?.let { put("transport", it) }
        }
        return Parsed(nameFrom(uri, "Trojan - $host"), "TROJAN", fullConfig(outbound))
    }

    private fun parseShadowsocks(link: String): Parsed {
        var body = link.substringAfter("://").substringBefore('#').substringBefore('?')
        if (!body.contains('@')) body = decodeBase64(body)
        val at = body.lastIndexOf('@')
        if (at < 0) throw IllegalArgumentException("لینک Shadowsocks نامعتبر است")
        var credentials = body.substring(0, at)
        val serverPart = body.substring(at + 1)
        if (!credentials.contains(':')) credentials = decodeBase64(credentials)
        val method = credentials.substringBefore(':')
        val password = credentials.substringAfter(':', "")
        if (method.isBlank() || password.isBlank()) throw IllegalArgumentException("روش رمزنگاری یا رمز Shadowsocks ناقص است")
        val serverUri = URI("ss://$serverPart")
        val host = serverUri.host ?: throw IllegalArgumentException("آدرس سرور Shadowsocks نامعتبر است")
        val port = serverUri.port.takeIf { it > 0 } ?: 8388
        val outbound = JSONObject().apply {
            put("type", "shadowsocks")
            put("tag", "proxy")
            put("server", host)
            put("server_port", port)
            put("method", method)
            put("password", password)
        }
        val label = link.substringAfter('#', "").takeIf { it.isNotBlank() }?.let(::decodePart)
        return Parsed(label ?: "Shadowsocks - $host", "SHADOWSOCKS", fullConfig(outbound))
    }

    private fun parseHysteria2(link: String): Parsed {
        val normalized = link.replaceFirst("hy2://", "hysteria2://", ignoreCase = true)
        val uri = URI(normalized)
        val password = uri.userInfo?.substringBefore(':')?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("رمز Hysteria2 وجود ندارد")
        val host = uri.host ?: throw IllegalArgumentException("آدرس سرور Hysteria2 نامعتبر است")
        val port = uri.port.takeIf { it > 0 } ?: 443
        val q = query(uri.rawQuery)
        val outbound = JSONObject().apply {
            put("type", "hysteria2")
            put("tag", "proxy")
            put("server", host)
            put("server_port", port)
            put("password", password)
            put("tls", tlsObject(host, q, false))
            q["obfs"]?.takeIf { it.isNotBlank() }?.let { obfs ->
                put("obfs", JSONObject().put("type", obfs).put("password", q["obfs-password"].orEmpty()))
            }
        }
        return Parsed(nameFrom(uri, "Hysteria2 - $host"), "HYSTERIA2", fullConfig(outbound))
    }

    private fun parseTuic(link: String): Parsed {
        val uri = URI(link)
        val auth = uri.userInfo.orEmpty().split(':', limit = 2)
        if (auth.size != 2 || auth[0].isBlank()) throw IllegalArgumentException("UUID یا رمز TUIC ناقص است")
        val host = uri.host ?: throw IllegalArgumentException("آدرس سرور TUIC نامعتبر است")
        val port = uri.port.takeIf { it > 0 } ?: 443
        val q = query(uri.rawQuery)
        val outbound = JSONObject().apply {
            put("type", "tuic")
            put("tag", "proxy")
            put("server", host)
            put("server_port", port)
            put("uuid", auth[0])
            put("password", auth[1])
            put("congestion_control", q["congestion_control"] ?: "bbr")
            put("tls", tlsObject(host, q, false))
        }
        return Parsed(nameFrom(uri, "TUIC - $host"), "TUIC", fullConfig(outbound))
    }

    private fun parseWireGuardIni(raw: String): Parsed {
        val sections = linkedMapOf<String, MutableMap<String, String>>()
        var section = ""
        raw.lines().forEach { line ->
            val clean = line.substringBefore('#').trim()
            if (clean.isBlank()) return@forEach
            if (clean.startsWith("[") && clean.endsWith("]")) {
                section = clean.substring(1, clean.length - 1).trim().lowercase()
                sections.putIfAbsent(section, linkedMapOf())
            } else if (section.isNotBlank() && clean.contains('=')) {
                val key = clean.substringBefore('=').trim().lowercase()
                val value = clean.substringAfter('=').trim()
                sections[section]?.set(key, value)
            }
        }
        val iface = sections["interface"] ?: throw IllegalArgumentException("بخش [Interface] پیدا نشد")
        val peer = sections["peer"] ?: throw IllegalArgumentException("بخش [Peer] پیدا نشد")
        val endpointAddress = peer["endpoint"] ?: throw IllegalArgumentException("Endpoint در فایل WireGuard وجود ندارد")
        val endpointUri = URI("udp://$endpointAddress")
        val host = endpointUri.host ?: throw IllegalArgumentException("آدرس Endpoint نامعتبر است")
        val port = endpointUri.port.takeIf { it > 0 } ?: 51820
        val privateKey = iface["privatekey"] ?: throw IllegalArgumentException("PrivateKey وجود ندارد")
        val publicKey = peer["publickey"] ?: throw IllegalArgumentException("PublicKey وجود ندارد")
        val addresses = (iface["address"] ?: throw IllegalArgumentException("Address وجود ندارد"))
            .split(',').map { it.trim() }.filter { it.isNotBlank() }
        val allowedIps = (peer["allowedips"] ?: "0.0.0.0/0, ::/0")
            .split(',').map { it.trim() }.filter { it.isNotBlank() }
        val peerConfig = JSONObject().apply {
            put("address", host)
            put("port", port)
            put("public_key", publicKey)
            put("allowed_ips", JSONArray(allowedIps))
            peer["presharedkey"]?.let { put("pre_shared_key", it) }
            peer["persistentkeepalive"]?.toIntOrNull()?.let { put("persistent_keepalive_interval", it) }
        }
        val endpoint = JSONObject().apply {
            put("type", "wireguard")
            put("tag", "proxy")
            put("system", false)
            put("mtu", 1408)
            put("address", JSONArray(addresses))
            put("private_key", privateKey)
            put("peers", JSONArray().put(peerConfig))
        }
        val label = iface["name"] ?: "WireGuard - $host"
        return Parsed(label, "WIREGUARD", fullConfigForEndpoint(endpoint))
    }

    private fun parseSingBoxJson(raw: String): Parsed {
        val root = JSONObject(raw)
        val outbounds = root.optJSONArray("outbounds") ?: JSONArray()
        val endpoints = root.optJSONArray("endpoints") ?: JSONArray()
        val primaryOutbound = (0 until outbounds.length()).mapNotNull { outbounds.optJSONObject(it) }
            .firstOrNull { it.optString("type") !in setOf("direct", "block", "dns") }
        val primaryEndpoint = (0 until endpoints.length()).mapNotNull { endpoints.optJSONObject(it) }.firstOrNull()
        val primary = primaryOutbound ?: primaryEndpoint ?: outbounds.optJSONObject(0)
            ?: throw IllegalArgumentException("هیچ outbound یا endpoint در JSON وجود ندارد")
        if (outbounds.length() == 0) {
            root.put("outbounds", JSONArray().put(JSONObject().put("type", "direct").put("tag", "direct"))
                .put(JSONObject().put("type", "block").put("tag", "block")))
        }
        val inbounds = root.optJSONArray("inbounds") ?: JSONArray()
        var hasTun = false
        for (i in 0 until inbounds.length()) {
            val inbound = inbounds.optJSONObject(i) ?: continue
            if (inbound.optString("type") == "tun") {
                inbound.put("auto_route", true)
                hasTun = true
            }
        }
        if (!hasTun) inbounds.put(tunInbound())
        root.put("inbounds", inbounds)

        if (!root.has("route")) {
            root.put("route", JSONObject().put("final", primary.optString("tag", "proxy")))
        }
        val route = root.optJSONObject("route") ?: JSONObject()
        if (!route.has("final")) route.put("final", primary.optString("tag", "proxy"))
        // The Android VPN service protects outbound sockets directly; desktop interface auto-detection is not used here.
        route.put("auto_detect_interface", false)
        root.put("route", route)
        if (!root.has("dns")) root.put("dns", defaultDns(primary.optString("tag", "proxy")))
        val protocol = primary.optString("type", "SING-BOX").uppercase()
        val name = primary.optString("tag").ifBlank { "Imported sing-box profile" }
        return Parsed(name, protocol, root.toString())
    }

    private fun fullConfig(outbound: JSONObject): String {
        val outbounds = JSONArray().put(outbound)
            .put(JSONObject().put("type", "direct").put("tag", "direct"))
            .put(JSONObject().put("type", "block").put("tag", "block"))
        return JSONObject()
            .put("log", JSONObject().put("level", "info").put("timestamp", true))
            .put("dns", defaultDns("proxy"))
            .put("inbounds", JSONArray().put(tunInbound()))
            .put("outbounds", outbounds)
            .put("route", JSONObject()
                .put("auto_detect_interface", false)
                .put("final", "proxy")
                .put("rules", JSONArray().put(JSONObject().put("protocol", "dns").put("action", "hijack-dns"))))
            .toString()
    }

    private fun fullConfigForEndpoint(endpoint: JSONObject): String {
        val outbounds = JSONArray()
            .put(JSONObject().put("type", "direct").put("tag", "direct"))
            .put(JSONObject().put("type", "block").put("tag", "block"))
        return JSONObject()
            .put("log", JSONObject().put("level", "info").put("timestamp", true))
            .put("dns", defaultDns("proxy"))
            .put("endpoints", JSONArray().put(endpoint))
            .put("inbounds", JSONArray().put(tunInbound()))
            .put("outbounds", outbounds)
            .put("route", JSONObject()
                .put("auto_detect_interface", false)
                .put("final", "proxy")
                .put("rules", JSONArray().put(JSONObject().put("protocol", "dns").put("action", "hijack-dns"))))
            .toString()
    }

    private fun tunInbound() = JSONObject()
        .put("type", "tun")
        .put("tag", "tun-in")
        .put("address", JSONArray().put("172.19.0.1/30").put("fdfe:dcba:9876::1/126"))
        .put("mtu", 9000)
        .put("auto_route", true)
        .put("stack", "mixed")

    private fun defaultDns(detour: String = "proxy") = JSONObject()
        .put("servers", JSONArray().put(JSONObject()
            .put("type", "https")
            .put("tag", "remote-dns")
            .put("server", "1.1.1.1")
            .put("path", "/dns-query")
            .put("detour", detour)))
        .put("final", "remote-dns")
        .put("strategy", "prefer_ipv4")

    private fun tlsObject(host: String, q: Map<String, String>, reality: Boolean): JSONObject {
        val tls = JSONObject().put("enabled", true)
            .put("server_name", q["sni"].orEmpty().ifBlank { q["serverName"].orEmpty().ifBlank { host } })
            .put("utls", JSONObject().put("enabled", true).put("fingerprint", q["fp"].orEmpty().ifBlank { "chrome" }))
        if (q["allowInsecure"] == "1" || q["insecure"] == "1") tls.put("insecure", true)
        if (reality) {
            val publicKey = q["pbk"].orEmpty().ifBlank { q["publicKey"].orEmpty() }
            if (publicKey.isBlank()) throw IllegalArgumentException("کلید عمومی Reality (pbk) در لینک نیست")
            tls.put("reality", JSONObject().put("enabled", true)
                .put("public_key", publicKey)
                .put("short_id", q["sid"].orEmpty()))
        }
        return tls
    }

    private fun transportObject(q: Map<String, String>): JSONObject? {
        return when (q["type"]?.lowercase()) {
            "ws", "websocket" -> JSONObject().put("type", "ws")
                .put("path", q["path"].orEmpty().ifBlank { "/" })
                .put("headers", JSONObject().put("Host", q["host"].orEmpty()))
            "grpc" -> JSONObject().put("type", "grpc").put("service_name", q["serviceName"].orEmpty().ifBlank { q["path"].orEmpty().removePrefix("/") })
            "http", "h2" -> JSONObject().put("type", "http")
                .put("host", JSONArray().put(q["host"].orEmpty()))
                .put("path", q["path"].orEmpty().ifBlank { "/" })
            "httpupgrade" -> JSONObject().put("type", "httpupgrade")
                .put("host", q["host"].orEmpty())
                .put("path", q["path"].orEmpty().ifBlank { "/" })
            else -> null
        }
    }

    private fun query(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split('&').mapNotNull { part ->
            if (part.isBlank()) return@mapNotNull null
            val key = part.substringBefore('=')
            val value = part.substringAfter('=', "")
            decodePart(key) to decodePart(value)
        }.toMap()
    }

    private fun nameFrom(uri: URI, fallback: String): String =
        uri.rawFragment?.takeIf { it.isNotBlank() }?.let(::decodePart)?.ifBlank { fallback } ?: fallback

    private fun decodePart(value: String): String = try {
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    } catch (_: Exception) { value }

    private fun decodeSubscription(input: String): String {
        val trimmed = input.trim()
        if (trimmed.startsWith("{") || trimmed.contains("://")) return trimmed
        val decoded = runCatching { decodeBase64(trimmed) }.getOrNull()
        return if (decoded != null && (decoded.contains("://") || decoded.trimStart().startsWith("{"))) decoded else trimmed
    }

    private fun decodeBase64(value: String): String {
        val normalized = value.trim().replace('-', '+').replace('_', '/')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        return String(AndroidBase64.decode(padded, AndroidBase64.DEFAULT), StandardCharsets.UTF_8)
    }
}
