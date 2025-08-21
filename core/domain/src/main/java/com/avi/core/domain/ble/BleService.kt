package com.avi.core.domain.ble

import kotlinx.coroutines.flow.Flow

/**
 * Service interface for managing BLE operations and device communication
 */
interface BleService {
    
    /**
     * Check if Bluetooth is enabled
     */
    suspend fun isBluetoothEnabled(): Boolean
    
    /**
     * Check if location permissions are granted
     */
    suspend fun hasLocationPermissions(): Boolean
    
    /**
     * Request necessary permissions
     */
    suspend fun requestPermissions(): Boolean
    
    /**
     * Start scanning for BLE devices
     */
    suspend fun startScan(): Result<Unit>
    
    /**
     * Stop scanning for BLE devices
     */
    suspend fun stopScan(): Result<Unit>
    
    /**
     * Get discovered devices
     */
    fun getDiscoveredDevices(): Flow<List<BleDevice>>
    
    /**
     * Connect to a specific BLE device
     */
    suspend fun connectToDevice(deviceAddress: String): Result<Unit>
    
    /**
     * Disconnect from a BLE device
     */
    suspend fun disconnectFromDevice(deviceAddress: String): Result<Unit>
    
    /**
     * Get connected devices
     */
    fun getConnectedDevices(): Flow<List<BleDevice>>
    
    /**
     * Discover services for a connected device
     */
    suspend fun discoverServices(deviceAddress: String): Result<List<BleServiceInfo>>
    
    /**
     * Read characteristic value
     */
    suspend fun readCharacteristic(
        deviceAddress: String,
        serviceUuid: String,
        characteristicUuid: String
    ): Result<ByteArray>
    
    /**
     * Write characteristic value
     */
    suspend fun writeCharacteristic(
        deviceAddress: String,
        serviceUuid: String,
        characteristicUuid: String,
        value: ByteArray
    ): Result<Unit>
    
    /**
     * Subscribe to characteristic notifications
     */
    suspend fun subscribeToCharacteristic(
        deviceAddress: String,
        serviceUuid: String,
        characteristicUuid: String
    ): Result<Unit>
    
    /**
     * Unsubscribe from characteristic notifications
     */
    suspend fun unsubscribeFromCharacteristic(
        deviceAddress: String,
        serviceUuid: String,
        characteristicUuid: String
    ): Result<Unit>
    
    /**
     * Get telemetry data stream from a device
     */
    fun getTelemetryDataStream(deviceAddress: String): Flow<TelemetryData>
    
    /**
     * Send command to embedded device
     */
    suspend fun sendCommand(
        deviceAddress: String,
        command: BleCommand
    ): Result<Unit>
    
    /**
     * Get device status and health information
     */
    suspend fun getDeviceStatus(deviceAddress: String): Result<DeviceStatus>
    
    /**
     * Configure device settings
     */
    suspend fun configureDevice(
        deviceAddress: String,
        configuration: DeviceConfiguration
    ): Result<Unit>
    
    /**
     * Get connection quality metrics
     */
    suspend fun getConnectionQuality(deviceAddress: String): Result<ConnectionQuality>
}

/**
 * Commands that can be sent to embedded devices
 */
sealed class BleCommand {
    abstract val commandId: String
    abstract val parameters: Map<String, Any>
}

data class StartDataCollectionCommand(
    override val commandId: String = "START_COLLECTION",
    override val parameters: Map<String, Any> = mapOf(
        "interval" to 1000, // milliseconds
        "sensors" to listOf("heart_rate", "accelerometer", "gps")
    )
) : BleCommand()

data class StopDataCollectionCommand(
    override val commandId: String = "STOP_COLLECTION",
    override val parameters: Map<String, Any> = emptyMap()
) : BleCommand()

data class ConfigureSensorCommand(
    val sensorId: String,
    val enabled: Boolean,
    val samplingRate: Int,
    override val commandId: String = "CONFIGURE_SENSOR",
    override val parameters: Map<String, Any> = mapOf(
        "sensor_id" to sensorId,
        "enabled" to enabled,
        "sampling_rate" to samplingRate
    )
) : BleCommand()

data class CalibrateSensorCommand(
    val sensorId: String,
    override val commandId: String = "CALIBRATE_SENSOR",
    override val parameters: Map<String, Any> = mapOf(
        "sensor_id" to sensorId
    )
) : BleCommand()

data class DeviceStatus(
    val deviceAddress: String,
    val isConnected: Boolean,
    val batteryLevel: Int,
    val signalStrength: Int,
    val lastSeen: Long,
    val firmwareVersion: String?,
    val hardwareVersion: String?,
    val supportedFeatures: List<String>
)

data class DeviceConfiguration(
    val deviceAddress: String,
    val sensorConfigurations: Map<String, SensorConfiguration>,
    val dataCollectionInterval: Int, // milliseconds
    val transmissionPower: Int, // dBm
    val sleepModeEnabled: Boolean
)

data class SensorConfiguration(
    val sensorId: String,
    val enabled: Boolean,
    val samplingRate: Int,
    val range: Float?,
    val resolution: Float?
)

data class ConnectionQuality(
    val deviceAddress: String,
    val rssi: Int,
    val connectionInterval: Int, // milliseconds
    val latency: Int, // milliseconds
    val throughput: Float, // bytes per second
    val packetLoss: Float, // percentage
    val lastUpdate: Long
)
