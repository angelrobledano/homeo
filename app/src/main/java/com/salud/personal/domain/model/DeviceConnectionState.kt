package com.salud.personal.domain.model

enum class DeviceConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    AUTHENTICATING,
    CONNECTED,
    AUTH_FAILED
}
