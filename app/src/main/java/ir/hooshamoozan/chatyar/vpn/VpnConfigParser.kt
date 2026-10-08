package ir.hooshamoozan.chatyar.vpn

import android.net.Uri
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

/**
 * Converts popular share formats to a single sing-box outbound.
 * Never place parsed configs, passwords or share links in logs/analytics.
 * Full sing-box JSON outbounds are also accepted for advanced protocols.
 */
object VpnConfigParser {
    private val formats = setOf("vless", "vmess", "trojan", "ss", "hy2", "hysteria2", "tuic", "anytls", "socks", "socks5", "http", "https")
    const val MAX_LENGTH = 256 * 1024

    fun identify(text: String): String {
        val value = text.trim()
        if (value.startsWith("{")) return "sing-box JSON"
        if (value.startsWith("proxies:") || value.startsWith("mixed-port:")) return "Clash YAML"
        val scheme = value.substringBefore("://", "").lowercase()
        if (scheme in formats) return scheme.uppercase()
        if (value.lines().size > 1 || looksBase64(value)) return "Subscription"
        throw IllegalArgumentException("فرمت ناشناخته است. لینک اشتراک، JSON یا لینک‌های پشتیبانی‌شده وارد کنید.")
    }

    /** Converts a single endpoint. Multi-line/base64 subscriptions use parseSubscription. */
    fun parse(text: String): JSONObject {
        val raw = text.trim()
        require(raw.length in 8..MAX_LENGTH) { "اندازه کانفیگ نامعتبر است" }
        if (raw.startsWith("{")) {
            val json = JSONObject(raw)
            if (json.has("outbounds")) {
                val outbounds = json.getJSONArray("outbounds")
                val item = (0 until outbounds.length()).map { outbounds.getJSONObject(it) }
                    .firstOrNull { it.optString("type") !in setOf("direct", "block", "dns", "selector", "urltest") }
                    ?: error("در فایل JSON هیچ outbound مناسبی پیدا نشد")
                return normalize(item)
            }
            if (json.has("type") && json.has("server")) return normalize(json)
            error("کانفیگ JSON باید outbound یا outbounds داشته باشد")
        }
        val scheme = raw.substringBefore("://", "").lowercase()
        require(scheme in formats) { "فرمت $scheme پشتیبانی نمی‌شود" }
        if (scheme == "vmess") return vmess(raw)
        if (scheme == "ss") return shadowsocks(raw)
        val url = Uri.parse(raw)
        val host = url.host?.takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("آدرس سرور معتبر نیست")
        val port = url.port.takeIf { it in 1..65535 } ?: error("پورت سرور معتبر نیست")
        val user = url.userInfoDecoded()
        val query = { key: String -> url.getQueryParameter(key).orEmpty() }
        val type = when (scheme) {
            "hy2" -> "hysteria2"
            "socks5" -> "socks"
            "https" -> "http"
            else -> scheme
        }
        val result = JSONObject().put("type", type).put("tag", "proxy").put("server", host).put("server_port", port)
        when (type) {
            "vless", "tuic" -> {
                require(user.isNotBlank()) { "UUID خالی است" }
                result.put("uuid", user.substringBefore(':'))
                if (type == "vless") result.put("flow", query("flow"))
                else result.put("password", user.substringAfter(':', query("password")))
            }
            "trojan", "hysteria2", "anytls" -> result.put("password", user)
            "socks", "http" -> {
                if (user.contains(':')) {
                    result.put("username", user.substringBefore(':'))
                    result.put("password", user.substringAfter(':'))
                } else if (user.isNotBlank()) result.put("username", user)
                if (type == "socks") result.put("version", "5")
            }
        }
        if (type in setOf("vless", "trojan", "hysteria2", "tuic", "anytls", "http")) {
            val secure = type in setOf("trojan", "hysteria2", "tuic", "anytls") || scheme == "https" ||
                query("security") in setOf("tls", "reality") || query("tls") == "1"
            if (secure) {
                val tls = JSONObject().put("enabled", true)
                val serverName = query("sni").ifBlank { query("peer").ifBlank { host } }
                tls.put("server_name", serverName)
                if (query("allowInsecure") == "1" || query("insecure") == "1") tls.put("insecure", true)
                val fingerprint = query("fp").ifBlank { query("fingerprint") }
                if (fingerprint.isNotBlank()) tls.put("utls", JSONObject().put("enabled", true).put("fingerprint", fingerprint))
                if (query("security") == "reality") {
                    val pub = query("pbk").ifBlank { query("publicKey") }
                    require(pub.isNotBlank()) { "REALITY public key لازم است" }
                    tls.put("reality", JSONObject().put("enabled", true).put("public_key", pub).put("short_id", query("sid")))
                }
                val alpn = query("alpn")
                if (alpn.isNotBlank()) tls.put("alpn", JSONArray(alpn.split(',')))
                result.put("tls", tls)
            }
        }
        val transport = query("type")
        if (transport in setOf("ws", "grpc", "http", "httpupgrade")) {
            val obj = JSONObject().put("type", transport)
            when (transport) {
                "ws", "http", "httpupgrade" -> {
                    obj.put("path", query("path").ifBlank { "/" })
                    val h = query("host")
                    if (h.isNotBlank()) obj.put("headers", JSONObject().put("Host", h))
                    if (transport == "http") obj.put("host", JSONArray(h.split(',').filter { it.isNotBlank() }))
                }
                "grpc" -> obj.put("service_name", query("serviceName").ifBlank { query("service_name") })
            }
            result.put("transport", obj)
        }
        if (type == "hysteria2") {
            val obfsPassword = query("obfs-password").ifBlank { query("obfsPassword") }
            if (obfsPassword.isNotBlank()) result.put("obfs", JSONObject().put("type", "salamander").put("password", obfsPassword))
        }
        return normalize(result)
    }

    fun parseSubscription(raw: String): List<JSONObject> {
        val text = raw.trim()
        require(text.length <= MAX_LENGTH) { "کانفیگ بیش از حد بزرگ است" }
        if (text.startsWith("proxies:") || text.startsWith("mixed-port:") ||
            text.contains("\nproxies:")) return parseClashYaml(text)
        if (text.startsWith("{")) return listOf(parse(text))
        val decoded = if (!text.contains("://") && looksBase64(text)) decode64(text) else text
        return decoded.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith('#') }
            .take(100).map { parse(it) }.toList().also { require(it.isNotEmpty()) { "کانفیگی یافت نشد" } }
    }

    fun singBoxConfig(raw: String): String {
        val outbound = parseSubscription(raw).first()
        return JSONObject().apply {
            put("log", JSONObject().put("level", "warn"))
            put("dns", JSONObject().put("servers", JSONArray().put(JSONObject().put("type", "https")
                .put("tag", "dns-remote").put("server", "1.1.1.1").put("path", "/dns-query")))
                .put("final", "dns-remote"))
            put("inbounds", JSONArray().put(JSONObject()
                .put("type", "tun").put("tag", "tun-in")
                .put("address", JSONArray().put("172.19.0.1/30").put("fdfe:dcba:9876::1/126"))
                .put("mtu", 1500).put("auto_route", true).put("dns_mode", "hijack")))
            put("outbounds", JSONArray().put(outbound).put(JSONObject().put("type", "direct").put("tag", "direct")))
            put("route", JSONObject().put("auto_detect_interface", true).put("final", "proxy"))
        }.toString()
    }

    private fun parseClashYaml(raw: String): List<JSONObject> {
        val options = LoaderOptions().apply {
            codePointLimit = MAX_LENGTH
            maxAliasesForCollections = 12
            setAllowDuplicateKeys(false)
        }
        val doc = Yaml(SafeConstructor(options)).load<Any>(raw) as? Map<*, *>
            ?: error("YAML نامعتبر است")
        val entries = doc["proxies"] as? List<*> ?: error("کلید proxies در YAML یافت نشد")
        return entries.take(100).map { entry ->
            val map = entry as? Map<*, *> ?: error("آیتم proxies نامعتبر است")
            fun value(key: String): String = map[key]?.toString().orEmpty()
            val type = when (value("type").lowercase()) {
                "ss" -> "shadowsocks"
                "socks5" -> "socks"
                "hy2" -> "hysteria2"
                else -> value("type").lowercase()
            }
            val server = value("server")
            val port = value("port").toIntOrNull() ?: 0
            val item = JSONObject().put("type", type).put("tag", "proxy")
                .put("server", server).put("server_port", port)
            when (type) {
                "vless", "vmess", "tuic" -> item.put("uuid", value("uuid"))
                "shadowsocks" -> {
                    item.put("method", value("cipher"))
                    item.put("password", value("password"))
                }
            }
            if (type in setOf("trojan", "hysteria2", "tuic", "anytls", "http", "socks")) {
                if (value("password").isNotBlank()) item.put("password", value("password"))
            }
            if (type == "vmess") {
                item.put("security", value("cipher").ifBlank { "auto" })
                item.put("alter_id", value("alterId").toIntOrNull() ?: 0)
            }
            val isTls = map["tls"] == true || type in setOf("trojan", "tuic", "hysteria2", "anytls")
            if (isTls) {
                val tls = JSONObject().put("enabled", true)
                    .put("server_name", value("servername").ifBlank { value("sni").ifBlank { server } })
                if (map["skip-cert-verify"] == true) tls.put("insecure", true)
                val reality = map["reality-opts"] as? Map<*, *>
                if (reality != null) {
                    tls.put("reality", JSONObject().put("enabled", true)
                        .put("public_key", reality["public-key"].orEmptyString())
                        .put("short_id", reality["short-id"].orEmptyString()))
                }
                item.put("tls", tls)
            }
            val network = value("network")
            if (network == "ws") {
                val ws = map["ws-opts"] as? Map<*, *>
                val tr = JSONObject().put("type", "ws")
                    .put("path", ws?.get("path").orEmptyString().ifBlank { "/" })
                val headers = ws?.get("headers") as? Map<*, *>
                if (headers != null) tr.put("headers", JSONObject(headers.mapKeys { it.key.toString() }))
                item.put("transport", tr)
            }
            if (network == "grpc") {
                val grpc = map["grpc-opts"] as? Map<*, *>
                item.put("transport", JSONObject().put("type", "grpc")
                    .put("service_name", grpc?.get("grpc-service-name").orEmptyString()))
            }
            normalize(item)
        }.also { require(it.isNotEmpty()) { "No proxies in YAML" } }
    }

    private fun Any?.orEmptyString(): String = this?.toString().orEmpty()

    private fun normalize(obj: JSONObject): JSONObject {
        obj.put("tag", "proxy")
        val type = obj.optString("type")
        require(type in setOf("vless", "vmess", "trojan", "shadowsocks", "hysteria2", "tuic", "anytls", "socks", "http", "ssh", "naive", "shadowtls")) {
            "outbound از نوع $type به‌صورت مستقل پشتیبانی نمی‌شود"
        }
        require(obj.optString("server").isNotBlank() && obj.optInt("server_port") in 1..65535) { "مشخصات سرور ناقص است" }
        return obj
    }

    private fun vmess(raw: String): JSONObject {
        val encoded = raw.substringAfter("://").substringBefore('#')
        val payload = JSONObject(decode64(encoded))
        val host = payload.optString("add")
        val port = payload.optString("port").toIntOrNull() ?: 0
        val out = JSONObject().put("type", "vmess").put("tag", "proxy").put("server", host)
            .put("server_port", port).put("uuid", payload.optString("id"))
            .put("security", payload.optString("scy", "auto")).put("alter_id", payload.optInt("aid", 0))
        if (payload.optString("tls") == "tls") out.put("tls", JSONObject().put("enabled", true)
            .put("server_name", payload.optString("sni").ifBlank { host }))
        val network = payload.optString("net")
        if (network in setOf("ws", "grpc", "http")) {
            val tr = JSONObject().put("type", network)
            if (network == "grpc") tr.put("service_name", payload.optString("path"))
            else {
                tr.put("path", payload.optString("path", "/"))
                if (payload.optString("host").isNotBlank()) tr.put("headers", JSONObject().put("Host", payload.optString("host")))
            }
            out.put("transport", tr)
        }
        return normalize(out)
    }

    private fun shadowsocks(raw: String): JSONObject {
        val whole = raw.substringAfter("://").substringBefore('#')
        val info = if (whole.contains('@')) whole else decode64(whole)
        val server = info.substringAfter('@', "")
        val credential = info.substringBefore('@')
        val details = if (credential.contains(':')) credential else decode64(credential)
        val method = details.substringBefore(':')
        val password = details.substringAfter(':', "")
        require(method.isNotBlank() && password.isNotBlank()) { "Shadowsocks method/password نامعتبر است" }
        val uri = Uri.parse("ss://x@$server")
        val host = uri.host ?: error("Shadowsocks host نامعتبر است")
        val port = uri.port
        return normalize(JSONObject().put("type", "shadowsocks").put("tag", "proxy")
            .put("server", host).put("server_port", port).put("method", method).put("password", password))
    }

    private fun Uri.userInfoDecoded(): String = userInfo.orEmpty()

    private fun looksBase64(text: String) = text.length > 30 && text.matches(Regex("[A-Za-z0-9+/=_\\-\\r\\n]+"))
    private fun decode64(encoded: String): String {
        val value = encoded.replace("\n", "").replace("\r", "")
        val variant = if (value.contains('-') || value.contains('_')) Base64.URL_SAFE else Base64.DEFAULT
        return String(Base64.decode(value, variant or Base64.NO_WRAP or Base64.NO_PADDING), Charsets.UTF_8)
    }
}
