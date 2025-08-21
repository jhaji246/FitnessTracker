package com.avi.core.domain.ble

/**
 * Represents a BLE device with its properties and connection state
 */
data class BleDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val isConnected: Boolean = false,
    val deviceType: BleDeviceType = BleDeviceType.UNKNOWN,
    val supportedServices: List<BleServiceInfo> = emptyList(),
    val lastSeen: Long = System.currentTimeMillis()
)

/**
 * Types of BLE devices supported by the application
 */
enum class BleDeviceType {
    FITNESS_TRACKER,
    HEART_RATE_MONITOR,
    SMART_WATCH,
    BIKE_SENSOR,
    RUNNING_SENSOR,
    UNKNOWN
}

/**
 * Represents a BLE service with its characteristics
 */
data class BleServiceInfo(
    val uuid: String,
    val name: String,
    val characteristics: List<BleCharacteristic> = emptyList()
)

/**
 * Represents a BLE characteristic with its properties
 */
data class BleCharacteristic(
    val uuid: String,
    val name: String,
    val properties: Int,
    val value: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleCharacteristic

        if (uuid != other.uuid) return false
        if (name != other.name) return false
        if (properties != other.properties) return false
        if (value != null) {
            if (other.value == null) return false
            if (!value.contentEquals(other.value)) return false
        } else if (other.value != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + properties
        result = 31 * result + (value?.contentHashCode() ?: 0)
        return result
    }
}
