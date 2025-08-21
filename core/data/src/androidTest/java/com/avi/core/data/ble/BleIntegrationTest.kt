package com.avi.core.data.ble

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.avi.core.domain.ble.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for BLE functionality
 * Tests the complete BLE service integration with real Android context
 */
@RunWith(AndroidJUnit4::class)
class BleIntegrationTest : KoinTest {

    private val bleService: BleService by inject()
    private val telemetryProcessor: TelemetryDataProcessor by inject()
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(context)
            modules(listOf(coreDataModule))
        }
    }

    @Test
    fun testBleServiceInjection() {
        // Test that BLE service can be injected
        assertNotNull(bleService, "BLE service should be injectable")
    }

    @Test
    fun testTelemetryProcessorInjection() {
        // Test that telemetry processor can be injected
        assertNotNull(telemetryProcessor, "Telemetry processor should be injectable")
    }

    @Test
    fun testBleServiceInterfaceCompliance() = runTest {
        // Test that BLE service implements all required methods
        val methods = bleService::class.java.methods.map { it.name }
        
        val requiredMethods = listOf(
            "isBluetoothEnabled",
            "hasLocationPermissions",
            "requestPermissions",
            "startScan",
            "stopScan",
            "getDiscoveredDevices",
            "connectToDevice",
            "disconnectFromDevice",
            "getConnectedDevices",
            "discoverServices",
            "readCharacteristic",
            "writeCharacteristic",
            "subscribeToCharacteristic",
            "unsubscribeFromCharacteristic",
            "getTelemetryDataStream",
            "sendCommand",
            "getDeviceStatus",
            "configureDevice",
            "getConnectionQuality"
        )
        
        requiredMethods.forEach { methodName ->
            assertTrue(
                methods.contains(methodName),
                "BLE service should have method: $methodName"
            )
        }
    }

    @Test
    fun testTelemetryDataFlowIntegration() = runTest {
        // Test complete telemetry data flow
        val deviceAddress = "test_device_001"
        
        // Create sample telemetry data
        val heartRateData = HeartRateData(
            timestamp = System.currentTimeMillis(),
            deviceAddress = deviceAddress,
            heartRate = 75
        )
        
        val gpsData = GpsLocationData(
            timestamp = System.currentTimeMillis() + 1000,
            deviceAddress = deviceAddress,
            latitude = 40.7128,
            longitude = -74.0060
        )
        
        // Process data through telemetry processor
        telemetryProcessor.processTelemetryData(deviceAddress, heartRateData)
        telemetryProcessor.processTelemetryData(deviceAddress, gpsData)
        
        // Verify processed data
        val processedData = telemetryProcessor.getProcessedData(deviceAddress).first()
        assertNotNull(processedData, "Processed data should not be null")
        assertEquals(1, processedData.heartRateData.size, "Should have 1 heart rate data point")
        assertEquals(1, processedData.gpsLocationData.size, "Should have 1 GPS data point")
        
        // Verify aggregated data
        val aggregatedData = telemetryProcessor.getAggregatedData(deviceAddress).first()
        assertNotNull(aggregatedData, "Aggregated data should not be null")
        assertEquals(deviceAddress, aggregatedData.deviceAddress, "Device address should match")
        assertEquals(2, aggregatedData.dataPoints.size, "Should have 2 total data points")
    }

    @Test
    fun testBleDeviceModelIntegration() {
        // Test BLE device model creation and validation
        val bleDevice = BleDevice(
            address = "00:11:22:33:44:55",
            name = "Test Fitness Tracker",
            rssi = -50,
            isConnected = false,
            deviceType = BleDeviceType.FITNESS_TRACKER,
            supportedServices = listOf(
                BleServiceInfo(
                    uuid = "180D",
                    name = "Heart Rate Service",
                    characteristics = listOf(
                        BleCharacteristic(
                            uuid = "2A37",
                            name = "Heart Rate Measurement",
                            properties = 16
                        )
                    )
                )
            ),
            lastSeen = System.currentTimeMillis()
        )
        
        // Verify device properties
        assertEquals("00:11:22:33:44:55", bleDevice.address, "Device address should match")
        assertEquals("Test Fitness Tracker", bleDevice.name, "Device name should match")
        assertEquals(-50, bleDevice.rssi, "RSSI should match")
        assertEquals(BleDeviceType.FITNESS_TRACKER, bleDevice.deviceType, "Device type should match")
        assertEquals(1, bleDevice.supportedServices.size, "Should have 1 supported service")
        assertEquals("180D", bleDevice.supportedServices[0].uuid, "Service UUID should match")
    }

    @Test
    fun testTelemetryDataModelIntegration() {
        // Test all telemetry data types
        val timestamp = System.currentTimeMillis()
        val deviceAddress = "test_device_002"
        
        // Heart Rate Data
        val heartRateData = HeartRateData(
            timestamp = timestamp,
            deviceAddress = deviceAddress,
            heartRate = 80,
            confidence = 0.95f,
            energyExpended = 150
        )
        assertEquals(TelemetryDataType.HEART_RATE, heartRateData.dataType, "Data type should be heart rate")
        assertEquals(80, heartRateData.heartRate, "Heart rate should match")
        
        // GPS Location Data
        val gpsData = GpsLocationData(
            timestamp = timestamp + 1000,
            deviceAddress = deviceAddress,
            latitude = 40.7128,
            longitude = -74.0060,
            altitude = 10.0,
            accuracy = 5.0f,
            speed = 2.5f
        )
        assertEquals(TelemetryDataType.GPS_LOCATION, gpsData.dataType, "Data type should be GPS location")
        assertEquals(40.7128, gpsData.latitude, "Latitude should match")
        assertEquals(-74.0060, gpsData.longitude, "Longitude should match")
        
        // Accelerometer Data
        val accelData = AccelerometerData(
            timestamp = timestamp + 2000,
            deviceAddress = deviceAddress,
            x = 1.5f,
            y = 2.0f,
            z = 0.5f
        )
        assertEquals(TelemetryDataType.ACCELEROMETER, accelData.dataType, "Data type should be accelerometer")
        assertTrue(accelData.magnitude > 0, "Magnitude should be calculated")
        
        // Battery Data
        val batteryData = BatteryData(
            timestamp = timestamp + 3000,
            deviceAddress = deviceAddress,
            level = 85,
            isCharging = false,
            voltage = 3.7f,
            temperature = 25.0f
        )
        assertEquals(TelemetryDataType.BATTERY_LEVEL, batteryData.dataType, "Data type should be battery")
        assertEquals(85, batteryData.level, "Battery level should match")
    }

    @Test
    fun testBleCommandIntegration() {
        // Test BLE command system
        val startCommand = StartDataCollectionCommand(
            commandId = "START_COLLECTION",
            parameters = mapOf(
                "interval" to 1000,
                "sensors" to listOf("heart_rate", "accelerometer", "gps")
            )
        )
        
        val stopCommand = StopDataCollectionCommand(
            commandId = "STOP_COLLECTION",
            parameters = emptyMap()
        )
        
        val configureCommand = ConfigureSensorCommand(
            sensorId = "heart_rate",
            enabled = true,
            samplingRate = 1000
        )
        
        // Verify command properties
        assertEquals("START_COLLECTION", startCommand.commandId, "Start command ID should match")
        assertEquals("STOP_COLLECTION", stopCommand.commandId, "Stop command ID should match")
        assertEquals("CONFIGURE_SENSOR", configureCommand.commandId, "Configure command ID should match")
        
        // Verify parameters
        assertEquals(1000, startCommand.parameters["interval"], "Start command interval should match")
        assertEquals("heart_rate", configureCommand.sensorId, "Configure command sensor ID should match")
    }

    @Test
    fun testDeviceConfigurationIntegration() {
        // Test device configuration system
        val sensorConfig = SensorConfiguration(
            sensorId = "accelerometer",
            enabled = true,
            samplingRate = 100,
            range = 16.0f,
            resolution = 0.01f
        )
        
        val deviceConfig = DeviceConfiguration(
            deviceAddress = "00:11:22:33:44:55",
            sensorConfigurations = mapOf("accelerometer" to sensorConfig),
            dataCollectionInterval = 1000,
            transmissionPower = 0,
            sleepModeEnabled = false
        )
        
        // Verify configuration properties
        assertEquals("00:11:22:33:44:55", deviceConfig.deviceAddress, "Device address should match")
        assertEquals(1, deviceConfig.sensorConfigurations.size, "Should have 1 sensor configuration")
        assertEquals(1000, deviceConfig.dataCollectionInterval, "Collection interval should match")
        assertEquals("accelerometer", deviceConfig.sensorConfigurations["accelerometer"]?.sensorId, "Sensor ID should match")
    }

    @Test
    fun testConnectionQualityIntegration() {
        // Test connection quality monitoring
        val connectionQuality = ConnectionQuality(
            deviceAddress = "00:11:22:33:44:55",
            rssi = -45,
            connectionInterval = 30,
            latency = 5,
            throughput = 1000.0f,
            packetLoss = 0.1f,
            lastUpdate = System.currentTimeMillis()
        )
        
        // Verify connection quality properties
        assertEquals("00:11:22:33:44:55", connectionQuality.deviceAddress, "Device address should match")
        assertEquals(-45, connectionQuality.rssi, "RSSI should match")
        assertEquals(30, connectionQuality.connectionInterval, "Connection interval should match")
        assertEquals(5, connectionQuality.latency, "Latency should match")
        assertEquals(1000.0f, connectionQuality.throughput, "Throughput should match")
        assertEquals(0.1f, connectionQuality.packetLoss, "Packet loss should match")
    }

    @Test
    fun testTelemetryStatisticsIntegration() {
        // Test telemetry statistics calculation
        val heartRateStats = HeartRateStatistics(
            min = 60,
            max = 120,
            average = 85.5f,
            variance = 225.0f
        )
        
        val locationStats = LocationStatistics(
            totalDistance = 5000.0f,
            averageSpeed = 2.5f,
            elevationGain = 100.0f,
            boundingBox = BoundingBox(
                minLat = 40.0,
                maxLat = 41.0,
                minLon = -75.0,
                maxLon = -74.0
            )
        )
        
        val movementStats = MovementStatistics(
            stepCount = 5000,
            caloriesBurned = 250.0f,
            activeTime = 3600000L // 1 hour
        )
        
        val batteryStats = BatteryStatistics(
            startLevel = 100,
            endLevel = 80,
            dischargeRate = 20.0f
        )
        
        val telemetryStats = TelemetryStatistics(
            heartRateStats = heartRateStats,
            locationStats = locationStats,
            movementStats = movementStats,
            batteryStats = batteryStats
        )
        
        // Verify statistics properties
        assertNotNull(telemetryStats.heartRateStats, "Heart rate stats should not be null")
        assertNotNull(telemetryStats.locationStats, "Location stats should not be null")
        assertNotNull(telemetryStats.movementStats, "Movement stats should not be null")
        assertNotNull(telemetryStats.batteryStats, "Battery stats should not be null")
        
        assertEquals(60, telemetryStats.heartRateStats?.min, "Min heart rate should match")
        assertEquals(5000.0f, telemetryStats.locationStats?.totalDistance, "Total distance should match")
        assertEquals(5000, telemetryStats.movementStats?.stepCount, "Step count should match")
        assertEquals(100, telemetryStats.batteryStats?.startLevel, "Start battery level should match")
    }
}
