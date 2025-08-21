package com.avi.core.data.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.avi.core.domain.ble.BleService
import com.avi.core.domain.ble.BleDevice
import com.avi.core.domain.ble.BleDeviceType
import com.avi.core.domain.ble.BleServiceInfo
import com.avi.core.domain.ble.BleCharacteristic
import com.avi.core.domain.ble.TelemetryData
import com.avi.core.domain.ble.HeartRateData
import com.avi.core.domain.ble.BatteryData
import com.avi.core.domain.ble.BleCommand
import com.avi.core.domain.ble.StartDataCollectionCommand
import com.avi.core.domain.ble.StopDataCollectionCommand
import com.avi.core.domain.ble.ConfigureSensorCommand
import com.avi.core.domain.ble.CalibrateSensorCommand
import com.avi.core.domain.ble.DeviceStatus
import com.avi.core.domain.ble.DeviceConfiguration
import com.avi.core.domain.ble.ConnectionQuality
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementation of BLE service for managing Bluetooth Low Energy operations
 * and communication with embedded devices
 */
class BleServiceImpl(
    private val context: Context
) : BleService {

    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }
    
    private val bluetoothLeScanner: BluetoothLeScanner? by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }

    // State management
    private val _discoveredDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    private val _connectedDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    private val _telemetryDataStreams = mutableMapOf<String, MutableStateFlow<TelemetryData>>()
    
    // Device connections
    private val gattConnections = mutableMapOf<String, BluetoothGatt>()
    
    // Scanning state
    private var isScanning = false
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val bleDevice = BleDevice(
                address = device.address,
                name = device.name,
                rssi = result.rssi,
                deviceType = determineDeviceType(device),
                lastSeen = System.currentTimeMillis()
            )
            
            updateDiscoveredDevice(bleDevice)
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE scan failed with error: $errorCode")
        }
    }

    override suspend fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    override suspend fun hasLocationPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    override suspend fun requestPermissions(): Boolean {
        // This would typically be handled by the UI layer
        // For now, we'll return false and let the UI handle permission requests
        return hasLocationPermissions()
    }

    override suspend fun startScan(): Result<Unit> {
        return try {
            if (!isBluetoothEnabled()) {
                return Result.failure(Exception("Bluetooth is not enabled"))
            }
            
            if (!hasLocationPermissions()) {
                return Result.failure(Exception("Location permissions not granted"))
            }
            
            if (isScanning) {
                return Result.success(Unit)
            }
            
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            
            val scanFilters = listOf(
                ScanFilter.Builder()
                    .build()
            )
            
            bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
            isScanning = true
            
            Log.d(TAG, "BLE scan started")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BLE scan", e)
            Result.failure(e)
        }
    }

    override suspend fun stopScan(): Result<Unit> {
        return try {
            if (!isScanning) {
                return Result.success(Unit)
            }
            
            bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false
            
            Log.d(TAG, "BLE scan stopped")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop BLE scan", e)
            Result.failure(e)
        }
    }

    override fun getDiscoveredDevices(): Flow<List<BleDevice>> = _discoveredDevices

    override suspend fun connectToDevice(deviceAddress: String): Result<Unit> {
        return try {
            val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
                ?: return Result.failure(Exception("Device not found"))
            
            val gatt = device.connectGatt(context, false, createGattCallback(deviceAddress))
            
            // Wait for connection with timeout
            withTimeout(10000) { // 10 seconds timeout
                suspendCancellableCoroutine<Unit> { continuation ->
                    gattConnections[deviceAddress] = gatt
                    continuation.invokeOnCancellation {
                        gatt.close()
                        gattConnections.remove(deviceAddress)
                    }
                }
            }
            
            Log.d(TAG, "Connected to device: $deviceAddress")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to device: $deviceAddress", e)
            Result.failure(e)
        }
    }

    override suspend fun disconnectFromDevice(deviceAddress: String): Result<Unit> {
        return try {
            val gatt = gattConnections[deviceAddress]
            gatt?.disconnect()
            gatt?.close()
            gattConnections.remove(deviceAddress)
            
            // Remove from connected devices
            val currentConnected = _connectedDevices.value.toMutableList()
            currentConnected.removeAll { it.address == deviceAddress }
            _connectedDevices.value = currentConnected
            
            Log.d(TAG, "Disconnected from device: $deviceAddress")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect from device: $deviceAddress", e)
            Result.failure(e)
        }
    }

    override fun getConnectedDevices(): Flow<List<BleDevice>> = _connectedDevices

    override suspend fun discoverServices(deviceAddress: String): Result<List<BleServiceInfo>> {
        return try {
            val gatt = gattConnections[deviceAddress]
                ?: return Result.failure(Exception("Device not connected"))
            
            val services = gatt.discoverServices()
            if (!services) {
                return Result.failure(Exception("Failed to discover services"))
            }
            
            // Wait for services to be discovered
            val discoveredServices = gatt.services.map { gattService ->
                BleServiceInfo(
                    uuid = gattService.uuid.toString(),
                    name = getServiceName(gattService.uuid),
                    characteristics = gattService.characteristics.map { gattChar ->
                        BleCharacteristic(
                            uuid = gattChar.uuid.toString(),
                            name = getCharacteristicName(gattChar.uuid),
                            properties = gattChar.properties,
                            value = gattChar.value
                        )
                    }
                )
            }
            
            Log.d(TAG, "Discovered ${discoveredServices.size} services for device: $deviceAddress")
            Result.success(discoveredServices)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to discover services for device: $deviceAddress", e)
            Result.failure(e)
        }
    }

    override suspend fun readCharacteristic(
        deviceAddress: String,
        serviceUuid: String,
        characteristicUuid: String
    ): Result<ByteArray> {
        return try {
            val gatt = gattConnections[deviceAddress]
                ?: return Result.failure(Exception("Device not connected"))
            
            val service = gatt.getService(UUID.fromString(serviceUuid))
                ?: return Result.failure(Exception("Service not found"))
            
            val characteristic = service.getCharacteristic(UUID.fromString(characteristicUuid))
                ?: return Result.failure(Exception("Characteristic not found"))
            
            if (!gatt.readCharacteristic(characteristic)) {
                return Result.failure(Exception("Failed to read characteristic"))
            }
            
            // Wait for read result
            return suspendCancellableCoroutine<Result<ByteArray>> { continuation ->
                // This would typically use a callback to get the result
                // For now, we'll return empty data
                continuation.resume(Result.success(ByteArray(0)))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read characteristic", e)
            Result.failure(e)
        }
    }

    override suspend fun writeCharacteristic(
        deviceAddress: String,
        serviceUuid: String,
        characteristicUuid: String,
        value: ByteArray
    ): Result<Unit> {
        return try {
            val gatt = gattConnections[deviceAddress]
                ?: return Result.failure(Exception("Device not connected"))
            
            val service = gatt.getService(UUID.fromString(serviceUuid))
                ?: return Result.failure(Exception("Service not found"))
            
            val characteristic = service.getCharacteristic(UUID.fromString(characteristicUuid))
                ?: return Result.failure(Exception("Characteristic not found"))
            
            characteristic.value = value
            
            if (!gatt.writeCharacteristic(characteristic)) {
                return Result.failure(Exception("Failed to write characteristic"))
            }
            
            Log.d(TAG, "Wrote ${value.size} bytes to characteristic: $characteristicUuid")
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write characteristic", e)
            Result.failure(e)
        }
    }

    override suspend fun subscribeToCharacteristic(
        deviceAddress: String,
        serviceUuid: String,
        characteristicUuid: String
    ): Result<Unit> {
        return try {
            val gatt = gattConnections[deviceAddress]
                ?: return Result.failure(Exception("Device not connected"))
            
            val service = gatt.getService(UUID.fromString(serviceUuid))
                ?: return Result.failure(Exception("Service not found"))
            
            val characteristic = service.getCharacteristic(UUID.fromString(characteristicUuid))
                ?: return Result.failure(Exception("Characteristic not found"))
            
            // Enable notifications
            gatt.setCharacteristicNotification(characteristic, true)
            
                    // Set descriptor for notifications
        val descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG))
        descriptor.value = byteArrayOf(0x01, 0x00) // Enable notifications
        gatt.writeDescriptor(descriptor)
            
            Log.d(TAG, "Subscribed to characteristic: $characteristicUuid")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to characteristic", e)
            Result.failure(e)
        }
    }

    override suspend fun unsubscribeFromCharacteristic(
        deviceAddress: String,
        serviceUuid: String,
        characteristicUuid: String
    ): Result<Unit> {
        return try {
            val gatt = gattConnections[deviceAddress]
                ?: return Result.failure(Exception("Device not connected"))
            
            val service = gatt.getService(UUID.fromString(serviceUuid))
                ?: return Result.failure(Exception("Service not found"))
            
            val characteristic = service.getCharacteristic(UUID.fromString(characteristicUuid))
                ?: return Result.failure(Exception("Characteristic not found"))
            
            // Disable notifications
            gatt.setCharacteristicNotification(characteristic, false)
            
            Log.d(TAG, "Unsubscribed from characteristic: $characteristicUuid")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unsubscribe from characteristic", e)
            Result.failure(e)
        }
    }

    override fun getTelemetryDataStream(deviceAddress: String): Flow<TelemetryData> {
        return _telemetryDataStreams.getOrPut(deviceAddress) { MutableStateFlow(HeartRateData(0, "", 0)) }
    }

    override suspend fun sendCommand(
        deviceAddress: String,
        command: BleCommand
    ): Result<Unit> {
        return try {
            // Convert command to byte array and send via characteristic
            val commandData = serializeCommand(command)
            
            // Send to command characteristic
            writeCharacteristic(
                deviceAddress,
                COMMAND_SERVICE_UUID,
                COMMAND_CHARACTERISTIC_UUID,
                commandData
            )
            
            Log.d(TAG, "Sent command: ${command.commandId} to device: $deviceAddress")
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send command to device: $deviceAddress", e)
            Result.failure(e)
        }
    }

    override suspend fun getDeviceStatus(deviceAddress: String): Result<DeviceStatus> {
        return try {
            val gatt = gattConnections[deviceAddress]
                ?: return Result.failure(Exception("Device not connected"))
            
            // Read device info characteristics
            val deviceInfo = readDeviceInfo(gatt)
            
            val status = DeviceStatus(
                deviceAddress = deviceAddress,
                isConnected = true,
                batteryLevel = deviceInfo.batteryLevel,
                signalStrength = deviceInfo.rssi,
                lastSeen = System.currentTimeMillis(),
                firmwareVersion = deviceInfo.firmwareVersion,
                hardwareVersion = deviceInfo.hardwareVersion,
                supportedFeatures = deviceInfo.supportedFeatures
            )
            
            Result.success(status)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get device status for: $deviceAddress", e)
            Result.failure(e)
        }
    }

    override suspend fun configureDevice(
        deviceAddress: String,
        configuration: DeviceConfiguration
    ): Result<Unit> {
        return try {
            // Send configuration command
            val configCommand = ConfigureSensorCommand(
                sensorId = "all",
                enabled = true,
                samplingRate = configuration.dataCollectionInterval
            )
            
            sendCommand(deviceAddress, configCommand)
            
            Log.d(TAG, "Configured device: $deviceAddress")
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure device: $deviceAddress", e)
            Result.failure(e)
        }
    }

    override suspend fun getConnectionQuality(deviceAddress: String): Result<ConnectionQuality> {
        return try {
            val gatt = gattConnections[deviceAddress]
                ?: return Result.failure(Exception("Device not connected"))
            
            val quality = ConnectionQuality(
                deviceAddress = deviceAddress,
                rssi = -50, // This would be read from the device
                connectionInterval = 30, // milliseconds
                latency = 5, // milliseconds
                throughput = 1000.0f, // bytes per second
                packetLoss = 0.1f, // percentage
                lastUpdate = System.currentTimeMillis()
            )
            
            return Result.success(quality)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get connection quality for: $deviceAddress", e)
            Result.failure(e)
        }
    }

    // Private helper methods
    private fun createGattCallback(deviceAddress: String): BluetoothGattCallback {
        return object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.d(TAG, "Connected to GATT server for device: $deviceAddress")
                        // Add to connected devices
                        val device = gatt.device
                        val bleDevice = BleDevice(
                            address = device.address,
                            name = device.name,
                            rssi = 0,
                            isConnected = true
                        )
                        updateConnectedDevice(bleDevice)
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.d(TAG, "Disconnected from GATT server for device: $deviceAddress")
                        // Remove from connected devices
                        val currentConnected = _connectedDevices.value.toMutableList()
                        currentConnected.removeAll { it.address == deviceAddress }
                        _connectedDevices.value = currentConnected
                    }
                }
            }
            
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Services discovered for device: $deviceAddress")
                } else {
                    Log.e(TAG, "Service discovery failed for device: $deviceAddress")
                }
            }
            
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                // Handle telemetry data updates
                handleTelemetryDataUpdate(deviceAddress, characteristic, value)
            }
        }
    }
    
    private fun updateDiscoveredDevice(device: BleDevice) {
        val current = _discoveredDevices.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.address == device.address }
        
        if (existingIndex >= 0) {
            current[existingIndex] = device
        } else {
            current.add(device)
        }
        
        _discoveredDevices.value = current
    }
    
    private fun updateConnectedDevice(device: BleDevice) {
        val current = _connectedDevices.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.address == device.address }
        
        if (existingIndex >= 0) {
            current[existingIndex] = device
        } else {
            current.add(device)
        }
        
        _connectedDevices.value = current
    }
    
    private fun determineDeviceType(device: BluetoothDevice): BleDeviceType {
        return when {
            device.name?.contains("fitness", ignoreCase = true) == true -> BleDeviceType.FITNESS_TRACKER
            device.name?.contains("heart", ignoreCase = true) == true -> BleDeviceType.HEART_RATE_MONITOR
            device.name?.contains("watch", ignoreCase = true) == true -> BleDeviceType.SMART_WATCH
            device.name?.contains("bike", ignoreCase = true) == true -> BleDeviceType.BIKE_SENSOR
            device.name?.contains("run", ignoreCase = true) == true -> BleDeviceType.RUNNING_SENSOR
            else -> BleDeviceType.UNKNOWN
        }
    }
    
    private fun getServiceName(uuid: UUID): String {
        return when (uuid.toString().uppercase()) {
            HEART_RATE_SERVICE_UUID -> "Heart Rate Service"
            BATTERY_SERVICE_UUID -> "Battery Service"
            DEVICE_INFO_SERVICE_UUID -> "Device Information Service"
            COMMAND_SERVICE_UUID -> "Command Service"
            else -> "Unknown Service"
        }
    }
    
    private fun getCharacteristicName(uuid: UUID): String {
        return when (uuid.toString().uppercase()) {
            HEART_RATE_MEASUREMENT_UUID -> "Heart Rate Measurement"
            BATTERY_LEVEL_UUID -> "Battery Level"
            FIRMWARE_REVISION_UUID -> "Firmware Revision"
            COMMAND_CHARACTERISTIC_UUID -> "Command"
            else -> "Unknown Characteristic"
        }
    }
    
    private fun serializeCommand(command: BleCommand): ByteArray {
        // Simple command serialization - in production, use proper protocol buffers or JSON
        val commandString = "${command.commandId}:${command.parameters.entries.joinToString(",") { "${it.key}=${it.value}" }}"
        return commandString.toByteArray()
    }
    
    private fun readDeviceInfo(gatt: BluetoothGatt): DeviceInfo {
        // This would read actual device information from characteristics
        return DeviceInfo(
            batteryLevel = 85,
            rssi = -45,
            firmwareVersion = "1.2.3",
            hardwareVersion = "2.1.0",
            supportedFeatures = listOf("heart_rate", "accelerometer", "gps")
        )
    }
    
    private fun handleTelemetryDataUpdate(
        deviceAddress: String,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        // Parse telemetry data and update streams
        val telemetryData = parseTelemetryData(deviceAddress, characteristic.uuid.toString(), value)
        telemetryData?.let { data ->
            _telemetryDataStreams[deviceAddress]?.value = data
        }
    }
    
    private fun parseTelemetryData(
        deviceAddress: String,
        characteristicUuid: String,
        value: ByteArray
    ): TelemetryData? {
        return when (characteristicUuid.uppercase()) {
            HEART_RATE_MEASUREMENT_UUID -> parseHeartRateData(deviceAddress, value)
            BATTERY_LEVEL_UUID -> parseBatteryData(deviceAddress, value)
            else -> null
        }
    }
    
    private fun parseHeartRateData(deviceAddress: String, value: ByteArray): TelemetryData? {
        return if (value.size >= 2) {
            val flags = value[0].toInt()
            val heartRate = if (flags and 0x01 == 0) {
                value[1].toInt()
            } else {
                (value[2].toInt() shl 8) or value[1].toInt()
            }
            
            HeartRateData(
                timestamp = System.currentTimeMillis(),
                deviceAddress = deviceAddress,
                heartRate = heartRate
            )
        } else null
    }
    
    private fun parseBatteryData(deviceAddress: String, value: ByteArray): TelemetryData? {
        return if (value.isNotEmpty()) {
            BatteryData(
                timestamp = System.currentTimeMillis(),
                deviceAddress = deviceAddress,
                level = value[0].toInt()
            )
        } else null
    }
    
    private data class DeviceInfo(
        val batteryLevel: Int,
        val rssi: Int,
        val firmwareVersion: String,
        val hardwareVersion: String,
        val supportedFeatures: List<String>
    )
    
    companion object {
        private const val TAG = "BleServiceImpl"
        
        // Standard BLE UUIDs
        private const val HEART_RATE_SERVICE_UUID = "180D"
        private const val BATTERY_SERVICE_UUID = "180F"
        private const val DEVICE_INFO_SERVICE_UUID = "180A"
        private const val COMMAND_SERVICE_UUID = "0000FFE0-0000-1000-8000-00805F9B34FB"
        
        private const val HEART_RATE_MEASUREMENT_UUID = "2A37"
        private const val BATTERY_LEVEL_UUID = "2A19"
        private const val FIRMWARE_REVISION_UUID = "2A26"
        private const val COMMAND_CHARACTERISTIC_UUID = "0000FFE1-0000-1000-8000-00805F9B34FB"
        
        private const val CLIENT_CHARACTERISTIC_CONFIG = "2902"
    }
}
