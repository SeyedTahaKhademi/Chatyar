package ir.hooshamoozan.chatyar.data

import android.content.Context
import ir.hooshamoozan.chatyar.vpn.VpnConfigParser
import java.util.UUID

data class VpnProfile(val id: String, val name: String, val technology: String)

/** API credentials and VPN node secrets are both encrypted with Android Keystore. */
class VpnProfileStore(context: Context, private val secretStore: SecretStore) {
    private val prefs = context.getSharedPreferences("chatyar_vpn_profiles", Context.MODE_PRIVATE)

    fun profiles(): List<VpnProfile> = prefs.getStringSet("ids", emptySet()).orEmpty().mapNotNull { id ->
        val name = prefs.getString("name_$id", null) ?: return@mapNotNull null
        val type = prefs.getString("type_$id", null) ?: return@mapNotNull null
        VpnProfile(id, name, type)
    }.sortedBy { it.name }

    fun save(name: String, content: String): VpnProfile = saveMany(name, content).first()

    /** Import explicit share links, base64 subscriptions and sing-box/Clash config documents. */
    fun saveMany(name: String, content: String): List<VpnProfile> {
        require(name.trim().isNotBlank()) { "نام پروفایل را وارد کنید" }
        val nodes = VpnConfigParser.parseSubscription(content)
        return nodes.mapIndexed { index, node ->
            val id = UUID.randomUUID().toString()
            val profileName = if (nodes.size == 1) name.trim() else "${name.trim()} • ${index + 1}"
            val type = node.getString("type").uppercase()
            secretStore.put("vpn_$id", node.toString())
            val ids = prefs.getStringSet("ids", emptySet()).orEmpty().toMutableSet().apply { add(id) }
            prefs.edit().putStringSet("ids", ids).putString("name_$id", profileName)
                .putString("type_$id", type).apply()
            VpnProfile(id, profileName, type)
        }
    }

    fun config(id: String): String = secretStore.get("vpn_$id")

    fun remove(id: String) {
        secretStore.remove("vpn_$id")
        val ids = prefs.getStringSet("ids", emptySet()).orEmpty().toMutableSet().apply { remove(id) }
        prefs.edit().putStringSet("ids", ids).remove("name_$id").remove("type_$id").apply()
    }
}
