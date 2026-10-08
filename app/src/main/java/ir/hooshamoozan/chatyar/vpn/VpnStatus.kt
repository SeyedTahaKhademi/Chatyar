package ir.hooshamoozan.chatyar.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class TunnelStatus {
    data object Disconnected : TunnelStatus()
    data class Connecting(val profileId: String) : TunnelStatus()
    data class Connected(val profileId: String) : TunnelStatus()
    data class Failed(val description: String) : TunnelStatus()
}

/** Process-local state; actual VPN existence always depends on the Android service. */
object VpnStatus {
    private val _state = MutableStateFlow<TunnelStatus>(TunnelStatus.Disconnected)
    val state: StateFlow<TunnelStatus> = _state
    fun setConnecting(id: String) { _state.value = TunnelStatus.Connecting(id) }
    fun setConnected(id: String) { _state.value = TunnelStatus.Connected(id) }
    fun setDisconnected() { _state.value = TunnelStatus.Disconnected }
    fun setError(message: String) { _state.value = TunnelStatus.Failed(message) }
}
