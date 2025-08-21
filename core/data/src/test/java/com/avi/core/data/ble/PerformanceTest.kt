package com.avi.core.data.ble

import com.avi.core.domain.ble.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Performance tests for BLE functionality
 * Tests performance characteristics and optimization
 */
class PerformanceTest {

    private lateinit var telemetryProcessor: TelemetryDataProcessor
    private val testDeviceAddress = "performance_test_device"

    @Before
    fun setUp() {
        telemetryProcessor = TelemetryDataProcessor()
    }

    @Test
    fun `test telemetry data processing performance`() = runTest {
        // Test processing performance with large datasets
        val dataPoints = 10000
        val telemetryData = generateLargeTelemetryDataset(dataPoints)
        
        val processingTime = measureTimeMillis {
            telemetryData.forEach { data ->
                telemetryProcessor.processTelemetryData(testDeviceAddress, data)
            }
        }
        
        // Performance assertion: Should process 10k data points in under 5 seconds
        assertTrue(processingTime < 5000, "Processing $dataPoints data points took $processingTime ms, expected < 5000 ms")
        
        // Verify data was processed correctly
        val processedData = telemetryProcessor.getProcessedData(testDeviceAddress).first()
        assertNotNull(processedData)
        assertEquals(dataPoints, processedData?.heartRateData?.size ?: 0, "All data points should be processed")
    }

    @Test
    fun `test memory efficiency with large datasets`() = runTest {
        // Test memory efficiency with very large datasets
        val largeDataset = 50000
        val telemetryData = generateLargeTelemetryDataset(largeDataset)
        
        // Measure memory usage before processing
        val runtime = Runtime.getRuntime()
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
        
        // Process large dataset
        telemetryData.forEach { data ->
            telemetryProcessor.processTelemetryData(testDeviceAddress, data)
        }
        
        // Measure memory usage after processing
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = memoryAfter - memoryBefore
        
        // Memory increase should be reasonable (less than 100MB for 50k data points)
        val memoryIncreaseMB = memoryIncrease / (1024 * 1024)
        assertTrue(memoryIncreaseMB < 100, "Memory increase was ${memoryIncreaseMB}MB, expected < 100MB")
        
        // Verify buffer limits are enforced
        val processedData = telemetryProcessor.getProcessedData(testDeviceAddress).first()
        assertNotNull(processedData)
        assertTrue((processedData?.heartRateData?.size ?: 0) <= 1000, "Buffer should maintain size limit")
    }

    @Test
    fun `test concurrent data processing performance`() = runTest {
        // Test concurrent processing performance
        val concurrentDevices = 10
        val dataPointsPerDevice = 1000
        
        val processingTime = measureTimeMillis {
            // Process data for multiple devices concurrently
            repeat(concurrentDevices) { deviceIndex ->
                val deviceAddress = "device_$deviceIndex"
                val deviceData = generateTelemetryDataForDevice(deviceAddress, dataPointsPerDevice)
                
                deviceData.forEach { data ->
                    telemetryProcessor.processTelemetryData(deviceAddress, data)
                }
            }
        }
        
        // Performance assertion: Should handle 10 devices with 1k data points each in under 3 seconds
        val totalDataPoints = concurrentDevices * dataPointsPerDevice
        assertTrue(processingTime < 3000, "Processing $totalDataPoints total data points took $processingTime ms, expected < 3000 ms")
        
        // Verify all devices were processed
        repeat(concurrentDevices) { deviceIndex ->
            val deviceAddress = "device_$deviceIndex"
            val processedData = telemetryProcessor.getProcessedData(deviceAddress).first()
            assertNotNull(processedData)
            assertEquals(dataPointsPerDevice, processedData?.heartRateData?.size ?: 0, "Device $deviceIndex should have all data processed")
        }
    }

    @Test
    fun `test statistics calculation performance`() = runTest {
        // Test statistics calculation performance
        val dataPoints = 5000
        val telemetryData = generateLargeTelemetryDataset(dataPoints)
        
        // Process data first
        telemetryData.forEach { data ->
            telemetryProcessor.processTelemetryData(testDeviceAddress, data)
        }
        
        // Measure statistics calculation time
        val calculationTime = measureTimeMillis {
            val aggregatedData = telemetryProcessor.getAggregatedData(testDeviceAddress).first()
            assertNotNull(aggregatedData?.statistics)
        }
        
        // Statistics calculation should be fast (under 100ms for 5k data points)
        assertTrue(calculationTime < 100, "Statistics calculation took $calculationTime ms, expected < 100 ms")
    }

    @Test
    fun `test data aggregation performance`() = runTest {
        // Test data aggregation performance
        val dataPoints = 10000
        val telemetryData = generateMixedTelemetryDataset(dataPoints)
        
        // Process mixed data types
        telemetryData.forEach { data ->
            telemetryProcessor.processTelemetryData(testDeviceAddress, data)
        }
        
        // Measure aggregation time
        val aggregationTime = measureTimeMillis {
            val aggregatedData = telemetryProcessor.getAggregatedData(testDeviceAddress).first()
            assertEquals(dataPoints, aggregatedData?.dataPoints?.size ?: 0, "All data points should be aggregated")
        }
        
        // Aggregation should be fast (under 200ms for 10k mixed data points)
        assertTrue(aggregationTime < 200, "Data aggregation took $aggregationTime ms, expected < 200 ms")
    }

    @Test
    fun `test step detection algorithm performance`() = runTest {
        // Test step detection algorithm performance
        val accelerometerData = generateAccelerometerDataForStepDetection(10000)
        
        val detectionTime = measureTimeMillis {
            accelerometerData.forEach { data ->
                telemetryProcessor.processTelemetryData(testDeviceAddress, data)
            }
        }
        
        // Step detection should be fast (under 1 second for 10k accelerometer readings)
        assertTrue(detectionTime < 1000, "Step detection took $detectionTime ms, expected < 1000 ms")
        
        // Verify step detection accuracy
        val processedData = telemetryProcessor.getProcessedData(testDeviceAddress).first()
        assertNotNull(processedData)
        assertTrue((processedData?.stepCount ?: 0) > 0, "Should detect some steps")
        assertTrue((processedData?.caloriesBurned ?: 0f) > 0f, "Should calculate calories from steps")
    }

    @Test
    fun `test activity recognition performance`() = runTest {
        // Test activity recognition performance
        val gyroscopeData = generateGyroscopeDataForActivityRecognition(5000)
        
        val recognitionTime = measureTimeMillis {
            gyroscopeData.forEach { data ->
                telemetryProcessor.processTelemetryData(testDeviceAddress, data)
            }
        }
        
        // Activity recognition should be fast (under 500ms for 5k gyroscope readings)
        assertTrue(recognitionTime < 500, "Activity recognition took $recognitionTime ms, expected < 500 ms")
        
        // Verify activity recognition
        val processedData = telemetryProcessor.getProcessedData(testDeviceAddress).first()
        assertNotNull(processedData)
        assertNotNull(processedData?.currentActivity)
        assertTrue(processedData?.activityDurations?.isNotEmpty() == true, "Should track activity durations")
    }

    @Test
    fun `test distance calculation performance`() = runTest {
        // Test GPS distance calculation performance
        val gpsData = generateGPSDataForDistanceCalculation(10000)
        
        val calculationTime = measureTimeMillis {
            gpsData.forEach { data ->
                telemetryProcessor.processTelemetryData(testDeviceAddress, data)
            }
        }
        
        // Distance calculation should be fast (under 1 second for 10k GPS points)
        assertTrue(calculationTime < 1000, "Distance calculation took $calculationTime ms, expected < 1000 ms")
        
        // Verify distance calculation
        val processedData = telemetryProcessor.getProcessedData(testDeviceAddress).first()
        assertNotNull(processedData)
        assertTrue((processedData?.totalDistance ?: 0f) > 0f, "Should calculate total distance")
        assertNotNull(processedData?.locationStats)
    }

    // Helper methods for generating test data
    private fun generateLargeTelemetryDataset(size: Int): List<TelemetryData> {
        return (1..size).map { index ->
            HeartRateData(
                timestamp = System.currentTimeMillis() + index,
                deviceAddress = testDeviceAddress,
                heartRate = 70 + (index % 30)
            )
        }
    }

    private fun generateTelemetryDataForDevice(deviceAddress: String, size: Int): List<TelemetryData> {
        return (1..size).map { index ->
            HeartRateData(
                timestamp = System.currentTimeMillis() + index,
                deviceAddress = deviceAddress,
                heartRate = 70 + (index % 30)
            )
        }
    }

    private fun generateMixedTelemetryDataset(size: Int): List<TelemetryData> {
        return (1..size).map { index ->
            when (index % 4) {
                0 -> HeartRateData(
                    timestamp = System.currentTimeMillis() + index,
                    deviceAddress = testDeviceAddress,
                    heartRate = 70 + (index % 30)
                )
                1 -> GpsLocationData(
                    timestamp = System.currentTimeMillis() + index,
                    deviceAddress = testDeviceAddress,
                    latitude = 40.0 + (index % 100) * 0.001,
                    longitude = -74.0 + (index % 100) * 0.001
                )
                2 -> AccelerometerData(
                    timestamp = System.currentTimeMillis() + index,
                    deviceAddress = testDeviceAddress,
                    x = (index % 20).toFloat(),
                    y = (index % 20).toFloat(),
                    z = (index % 20).toFloat()
                )
                else -> BatteryData(
                    timestamp = System.currentTimeMillis() + index,
                    deviceAddress = testDeviceAddress,
                    level = 100 - (index % 20)
                )
            }
        }
    }

    private fun generateAccelerometerDataForStepDetection(size: Int): List<TelemetryData> {
        return (1..size).map { index ->
            val magnitude = if (index % 100 < 10) 25.0f else 5.0f // Simulate step pattern
            AccelerometerData(
                timestamp = System.currentTimeMillis() + index,
                deviceAddress = testDeviceAddress,
                x = magnitude * 0.5f,
                y = magnitude * 0.3f,
                z = magnitude * 0.8f
            )
        }
    }

    private fun generateGyroscopeDataForActivityRecognition(size: Int): List<TelemetryData> {
        return (1..size).map { index ->
            val magnitude = when (index % 100) {
                in 0..20 -> 0.2f // Stationary
                in 21..40 -> 1.0f // Walking
                in 41..60 -> 2.5f // Running
                in 61..80 -> 4.0f // Cycling
                else -> 6.0f // High intensity
            }
            GyroscopeData(
                timestamp = System.currentTimeMillis() + index,
                deviceAddress = testDeviceAddress,
                x = magnitude * 0.4f,
                y = magnitude * 0.3f,
                z = magnitude * 0.3f
            )
        }
    }

    private fun generateGPSDataForDistanceCalculation(size: Int): List<TelemetryData> {
        return (1..size).map { index ->
            GpsLocationData(
                timestamp = System.currentTimeMillis() + index,
                deviceAddress = testDeviceAddress,
                latitude = 40.0 + (index * 0.0001), // Small increments for realistic movement
                longitude = -74.0 + (index * 0.0001)
            )
        }
    }

    private fun assertNotNull(any: Any?) {
        assert(any != null) { "Expected non-null value" }
    }
}
