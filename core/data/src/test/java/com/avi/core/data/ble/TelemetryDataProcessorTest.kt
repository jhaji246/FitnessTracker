package com.avi.core.data.ble

import com.avi.core.domain.ble.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TelemetryDataProcessorTest {

    private lateinit var telemetryDataProcessor: TelemetryDataProcessor
    private val testDeviceAddress = "00:11:22:33:44:55"

    @Before
    fun setUp() {
        telemetryDataProcessor = TelemetryDataProcessor()
    }

    @Test
    fun `test processHeartRateData updates statistics correctly`() = runTest {
        // Given
        val heartRateData1 = HeartRateData(
            timestamp = System.currentTimeMillis(),
            deviceAddress = testDeviceAddress,
            heartRate = 75
        )
        val heartRateData2 = HeartRateData(
            timestamp = System.currentTimeMillis() + 1000,
            deviceAddress = testDeviceAddress,
            heartRate = 85
        )

        // When
        telemetryDataProcessor.processTelemetryData(testDeviceAddress, heartRateData1)
        telemetryDataProcessor.processTelemetryData(testDeviceAddress, heartRateData2)

        // Then
        val processedData = telemetryDataProcessor.getProcessedData(testDeviceAddress).first()
        assertNotNull(processedData)
        assertEquals(2, processedData.heartRateData.size)
        assertNotNull(processedData.heartRateStats)
        assertEquals(75, processedData.heartRateStats?.min)
        assertEquals(85, processedData.heartRateStats?.max)
        assertEquals(80.0f, processedData.heartRateStats?.average)
    }

    @Test
    fun `test processGpsLocationData calculates distance correctly`() = runTest {
        // Given
        val location1 = GpsLocationData(
            timestamp = System.currentTimeMillis(),
            deviceAddress = testDeviceAddress,
            latitude = 40.7128,
            longitude = -74.0060
        )
        val location2 = GpsLocationData(
            timestamp = System.currentTimeMillis() + 1000,
            deviceAddress = testDeviceAddress,
            latitude = 40.7129,
            longitude = -74.0061
        )

        // When
        telemetryDataProcessor.processTelemetryData(testDeviceAddress, location1)
        telemetryDataProcessor.processTelemetryData(testDeviceAddress, location2)

        // Then
        val processedData = telemetryDataProcessor.getProcessedData(testDeviceAddress).first()
        assertNotNull(processedData)
        assertEquals(2, processedData.gpsLocationData.size)
        assertTrue(processedData.totalDistance > 0f)
        assertNotNull(processedData.locationStats)
    }

    @Test
    fun `test processAccelerometerData detects steps correctly`() = runTest {
        // Given
        val lowAccel = AccelerometerData(
            timestamp = System.currentTimeMillis(),
            deviceAddress = testDeviceAddress,
            x = 1.0f,
            y = 1.0f,
            z = 1.0f
        )
        val highAccel = AccelerometerData(
            timestamp = System.currentTimeMillis() + 100,
            deviceAddress = testDeviceAddress,
            x = 20.0f,
            y = 20.0f,
            z = 20.0f
        )

        // When
        telemetryDataProcessor.processTelemetryData(testDeviceAddress, lowAccel)
        telemetryDataProcessor.processTelemetryData(testDeviceAddress, highAccel)

        // Then
        val processedData = telemetryDataProcessor.getProcessedData(testDeviceAddress).first()
        assertNotNull(processedData)
        assertEquals(1, processedData.stepCount)
        assertTrue(processedData.caloriesBurned > 0f)
    }

    @Test
    fun `test processGyroscopeData detects activity type correctly`() = runTest {
        // Given
        val lowGyro = GyroscopeData(
            timestamp = System.currentTimeMillis(),
            deviceAddress = testDeviceAddress,
            x = 0.1f,
            y = 0.1f,
            z = 0.1f
        )
        val highGyro = GyroscopeData(
            timestamp = System.currentTimeMillis() + 100,
            deviceAddress = testDeviceAddress,
            x = 5.0f,
            y = 5.0f,
            z = 5.0f
        )

        // When
        telemetryDataProcessor.processTelemetryData(testDeviceAddress, lowGyro)
        telemetryDataProcessor.processTelemetryData(testDeviceAddress, highGyro)

        // Then
        val processedData = telemetryDataProcessor.getProcessedData(testDeviceAddress).first()
        assertNotNull(processedData)
        assertEquals(ActivityType.HIGH_INTENSITY, processedData.currentActivity)
    }

    @Test
    fun `test processBatteryData tracks battery statistics`() = runTest {
        // Given
        val battery1 = BatteryData(
            timestamp = System.currentTimeMillis(),
            deviceAddress = testDeviceAddress,
            level = 100
        )
        val battery2 = BatteryData(
            timestamp = System.currentTimeMillis() + 3600000, // 1 hour later
            deviceAddress = testDeviceAddress,
            level = 80
        )

        // When
        telemetryDataProcessor.processTelemetryData(testDeviceAddress, battery1)
        telemetryDataProcessor.processTelemetryData(testDeviceAddress, battery2)

        // Then
        val processedData = telemetryDataProcessor.getProcessedData(testDeviceAddress).first()
        assertNotNull(processedData)
        assertEquals(2, processedData.batteryData.size)
        assertNotNull(processedData.batteryStats)
        assertEquals(100, processedData.batteryStats?.startLevel)
        assertEquals(80, processedData.batteryStats?.endLevel)
        assertTrue(processedData.batteryStats?.dischargeRate!! > 0f)
    }

    @Test
    fun `test processEnvironmentalData stores data correctly`() = runTest {
        // Given
        val envData = EnvironmentalData(
            timestamp = System.currentTimeMillis(),
            deviceAddress = testDeviceAddress,
            temperature = 25.5f,
            humidity = 60.0f,
            pressure = 1013.25f
        )

        // When
        telemetryDataProcessor.processTelemetryData(testDeviceAddress, envData)

        // Then
        val processedData = telemetryDataProcessor.getProcessedData(testDeviceAddress).first()
        assertNotNull(processedData)
        assertEquals(1, processedData.environmentalData.size)
        assertEquals(25.5f, processedData.environmentalData[0].temperature)
        assertEquals(60.0f, processedData.environmentalData[0].humidity)
        assertEquals(1013.25f, processedData.environmentalData[0].pressure)
    }

    @Test
    fun `test processCustomSensorData handles various sensor types`() = runTest {
        // Given
        val customData = CustomSensorData(
            timestamp = System.currentTimeMillis(),
            deviceAddress = testDeviceAddress,
            sensorId = "custom_sensor_1",
            values = mapOf("value1" to 10.5f, "value2" to 20.3f),
            unit = "units"
        )

        // When
        telemetryDataProcessor.processTelemetryData(testDeviceAddress, customData)

        // Then
        val processedData = telemetryDataProcessor.getProcessedData(testDeviceAddress).first()
        assertNotNull(processedData)
        assertEquals(1, processedData.customSensorData.size)
        assertEquals("custom_sensor_1", processedData.customSensorData[0].sensorId)
        assertEquals(2, processedData.customSensorData[0].values.size)
        assertEquals("units", processedData.customSensorData[0].unit)
    }

    @Test
    fun `test aggregated data includes all telemetry types`() = runTest {
        // Given
        val heartRateData = HeartRateData(
            timestamp = System.currentTimeMillis(),
            deviceAddress = testDeviceAddress,
            heartRate = 75
        )
        val gpsData = GpsLocationData(
            timestamp = System.currentTimeMillis() + 1000,
            deviceAddress = testDeviceAddress,
            latitude = 40.7128,
            longitude = -74.0060
        )

        // When
        telemetryDataProcessor.processTelemetryData(testDeviceAddress, heartRateData)
        telemetryDataProcessor.processTelemetryData(testDeviceAddress, gpsData)

        // Then
        val aggregatedData = telemetryDataProcessor.getAggregatedData(testDeviceAddress).first()
        assertNotNull(aggregatedData)
        assertEquals(testDeviceAddress, aggregatedData.deviceAddress)
        assertEquals(2, aggregatedData.dataPoints.size)
        assertNotNull(aggregatedData.statistics.heartRateStats)
        assertNotNull(aggregatedData.statistics.locationStats)
    }

    @Test
    fun `test data buffer limits prevent memory issues`() = runTest {
        // Given - Create more than 1000 data points
        val manyDataPoints = (1..1100).map { index ->
            HeartRateData(
                timestamp = System.currentTimeMillis() + index,
                deviceAddress = testDeviceAddress,
                heartRate = 70 + (index % 30)
            )
        }

        // When
        manyDataPoints.forEach { data ->
            telemetryDataProcessor.processTelemetryData(testDeviceAddress, data)
        }

        // Then
        val processedData = telemetryDataProcessor.getProcessedData(testDeviceAddress).first()
        assertNotNull(processedData)
        // Should maintain buffer limit
        assertTrue(processedData.heartRateData.size <= 1000)
        // Should keep most recent data
        assertTrue(processedData.heartRateData.isNotEmpty())
    }

    @Test
    fun `test multiple devices are processed independently`() = runTest {
        // Given
        val device1 = "device_1"
        val device2 = "device_2"
        val heartRate1 = HeartRateData(
            timestamp = System.currentTimeMillis(),
            deviceAddress = device1,
            heartRate = 75
        )
        val heartRate2 = HeartRateData(
            timestamp = System.currentTimeMillis(),
            deviceAddress = device2,
            heartRate = 85
        )

        // When
        telemetryDataProcessor.processTelemetryData(device1, heartRate1)
        telemetryDataProcessor.processTelemetryData(device2, heartRate2)

        // Then
        val processedData1 = telemetryDataProcessor.getProcessedData(device1).first()
        val processedData2 = telemetryDataProcessor.getProcessedData(device2).first()
        
        assertNotNull(processedData1)
        assertNotNull(processedData2)
        assertEquals(1, processedData1.heartRateData.size)
        assertEquals(1, processedData2.heartRateData.size)
        assertEquals(75, processedData1.heartRateData[0].heartRate)
        assertEquals(85, processedData2.heartRateData[0].heartRate)
    }
}
