package com.avi.core.data.ble

import com.avi.core.domain.ble.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.atan2

/**
 * Processes telemetry data from embedded devices and provides aggregated insights
 */
class TelemetryDataProcessor {
    
    private val _processedData = MutableStateFlow<Map<String, ProcessedDeviceData>>(emptyMap())
    private val _aggregatedData = MutableStateFlow<Map<String, AggregatedTelemetryData>>(emptyMap())
    
    /**
     * Get processed data for a specific device
     */
    fun getProcessedData(deviceAddress: String): Flow<ProcessedDeviceData?> {
        return _processedData.map { it[deviceAddress] }
    }
    
    /**
     * Get aggregated data for a specific device
     */
    fun getAggregatedData(deviceAddress: String): Flow<AggregatedTelemetryData?> {
        return _aggregatedData.map { it[deviceAddress] }
    }
    
    /**
     * Process incoming telemetry data
     */
    fun processTelemetryData(deviceAddress: String, data: TelemetryData) {
        val currentProcessed = _processedData.value.toMutableMap()
        val deviceData = currentProcessed.getOrPut(deviceAddress) { ProcessedDeviceData(deviceAddress) }
        
        when (data) {
            is HeartRateData -> processHeartRateData(deviceData, data)
            is GpsLocationData -> processGpsLocationData(deviceData, data)
            is AccelerometerData -> processAccelerometerData(deviceData, data)
            is GyroscopeData -> processGyroscopeData(deviceData, data)
            is BatteryData -> processBatteryData(deviceData, data)
            is EnvironmentalData -> processEnvironmentalData(deviceData, data)
            is CustomSensorData -> processCustomSensorData(deviceData, data)
        }
        
        currentProcessed[deviceAddress] = deviceData
        _processedData.value = currentProcessed
        
        // Update aggregated data
        updateAggregatedData(deviceAddress, deviceData)
    }
    
    /**
     * Process heart rate data
     */
    private fun processHeartRateData(deviceData: ProcessedDeviceData, data: HeartRateData) {
        deviceData.heartRateData.add(data)
        
        // Keep only last 1000 heart rate readings
        if (deviceData.heartRateData.size > 1000) {
            deviceData.heartRateData.clear()
            deviceData.heartRateData.addAll(deviceData.heartRateData.takeLast(1000))
        }
        
        // Update statistics
        deviceData.heartRateStats = calculateHeartRateStatistics(deviceData.heartRateData)
        
        Timber.d("Processed heart rate data: ${data.heartRate} BPM for device: ${data.deviceAddress}")
    }
    
    /**
     * Process GPS location data
     */
    private fun processGpsLocationData(deviceData: ProcessedDeviceData, data: GpsLocationData) {
        deviceData.gpsLocationData.add(data)
        
        // Keep only last 1000 GPS readings
        if (deviceData.gpsLocationData.size > 1000) {
            deviceData.gpsLocationData.clear()
            deviceData.gpsLocationData.addAll(deviceData.gpsLocationData.takeLast(1000))
        }
        
        // Calculate distance and speed
        if (deviceData.gpsLocationData.size > 1) {
            val previousLocation = deviceData.gpsLocationData[deviceData.gpsLocationData.size - 2]
            val distance = calculateDistance(
                previousLocation.latitude, previousLocation.longitude,
                data.latitude, data.longitude
            ).toFloat()
            deviceData.totalDistance += distance
            
            // Calculate speed if time difference is available
            val timeDiff = data.timestamp - previousLocation.timestamp
            if (timeDiff > 0) {
                            val speed = (distance / (timeDiff / 1000.0)).toFloat() // m/s
            deviceData.currentSpeed = speed
            deviceData.speedHistory.add(speed)
                
                // Keep only last 100 speed readings
                if (deviceData.speedHistory.size > 100) {
                    deviceData.speedHistory.clear()
                    deviceData.speedHistory.addAll(deviceData.speedHistory.takeLast(100))
                }
            }
        }
        
        // Update location statistics
        deviceData.locationStats = calculateLocationStatistics(deviceData.gpsLocationData)
        
        Timber.d("Processed GPS data: ${data.latitude}, ${data.longitude} for device: ${data.deviceAddress}")
    }
    
    /**
     * Process accelerometer data
     */
    private fun processAccelerometerData(deviceData: ProcessedDeviceData, data: AccelerometerData) {
        deviceData.accelerometerData.add(data)
        
        // Keep only last 1000 accelerometer readings
        if (deviceData.accelerometerData.size > 1000) {
            deviceData.accelerometerData.clear()
            deviceData.accelerometerData.addAll(deviceData.accelerometerData.takeLast(1000))
        }
        
        // Detect steps using accelerometer data
        detectSteps(deviceData, data)
        
        // Calculate movement statistics
        deviceData.movementStats = calculateMovementStatistics(deviceData)
        
        Timber.d("Processed accelerometer data: ${data.magnitude} m/s² for device: ${data.deviceAddress}")
    }
    
    /**
     * Process gyroscope data
     */
    private fun processGyroscopeData(deviceData: ProcessedDeviceData, data: GyroscopeData) {
        deviceData.gyroscopeData.add(data)
        
        // Keep only last 1000 gyroscope readings
        if (deviceData.gyroscopeData.size > 1000) {
            deviceData.gyroscopeData.clear()
            deviceData.gyroscopeData.addAll(deviceData.gyroscopeData.takeLast(1000))
        }
        
        // Detect activity type using gyroscope data
        detectActivityType(deviceData, data)
        
        Timber.d("Processed gyroscope data: ${data.magnitude} rad/s for device: ${data.deviceAddress}")
    }
    
    /**
     * Process battery data
     */
    private fun processBatteryData(deviceData: ProcessedDeviceData, data: BatteryData) {
        deviceData.batteryData.add(data)
        
        // Keep only last 100 battery readings
        if (deviceData.batteryData.size > 100) {
            deviceData.batteryData.clear()
            deviceData.batteryData.addAll(deviceData.batteryData.takeLast(100))
        }
        
        // Calculate battery statistics
        deviceData.batteryStats = calculateBatteryStatistics(deviceData.batteryData)
        
        Timber.d("Processed battery data: ${data.level}% for device: ${data.deviceAddress}")
    }
    
    /**
     * Process environmental data
     */
    private fun processEnvironmentalData(deviceData: ProcessedDeviceData, data: EnvironmentalData) {
        deviceData.environmentalData.add(data)
        
        // Keep only last 100 environmental readings
        if (deviceData.environmentalData.size > 100) {
            deviceData.environmentalData.clear()
            deviceData.environmentalData.addAll(deviceData.environmentalData.takeLast(100))
        }
        
        Timber.d("Processed environmental data for device: ${data.deviceAddress}")
    }
    
    /**
     * Process custom sensor data
     */
    private fun processCustomSensorData(deviceData: ProcessedDeviceData, data: CustomSensorData) {
        deviceData.customSensorData.add(data)
        
        // Keep only last 100 custom sensor readings
        if (deviceData.customSensorData.size > 100) {
            deviceData.customSensorData.clear()
            deviceData.customSensorData.addAll(deviceData.customSensorData.takeLast(100))
        }
        
        Timber.d("Processed custom sensor data: ${data.sensorId} for device: ${data.deviceAddress}")
    }
    
    /**
     * Detect steps using accelerometer data
     */
    private fun detectSteps(deviceData: ProcessedDeviceData, data: AccelerometerData) {
        // Simple step detection algorithm
        val magnitude = data.magnitude
        val threshold = 15.0f // m/s² threshold for step detection
        
        if (magnitude > threshold && !deviceData.isStepDetected) {
            deviceData.stepCount++
            deviceData.isStepDetected = true
            deviceData.lastStepTime = data.timestamp
            
            // Calculate calories burned (rough estimation)
            val caloriesPerStep = 0.04f // calories per step
            deviceData.caloriesBurned += caloriesPerStep
            
            Timber.d("Step detected! Total steps: ${deviceData.stepCount}")
        } else if (magnitude < threshold * 0.5f) {
            deviceData.isStepDetected = false
        }
    }
    
    /**
     * Detect activity type using gyroscope data
     */
    private fun detectActivityType(deviceData: ProcessedDeviceData, data: GyroscopeData) {
        val magnitude = data.magnitude
        val threshold = 2.0f // rad/s threshold for activity detection
        
        when {
            magnitude < 0.5f -> deviceData.currentActivity = ActivityType.STATIONARY
            magnitude < 1.5f -> deviceData.currentActivity = ActivityType.WALKING
            magnitude < 3.0f -> deviceData.currentActivity = ActivityType.RUNNING
            magnitude < 5.0f -> deviceData.currentActivity = ActivityType.CYCLING
            else -> deviceData.currentActivity = ActivityType.HIGH_INTENSITY
        }
        
        // Update activity duration
        val currentTime = data.timestamp
        if (deviceData.currentActivity != deviceData.previousActivity) {
            deviceData.activityDurations[deviceData.previousActivity] = 
                deviceData.activityDurations.getOrDefault(deviceData.previousActivity, 0L) + 
                (currentTime - deviceData.lastActivityChangeTime)
            
            deviceData.previousActivity = deviceData.currentActivity
            deviceData.lastActivityChangeTime = currentTime
        }
    }
    
    /**
     * Calculate heart rate statistics
     */
    private fun calculateHeartRateStatistics(data: List<HeartRateData>): HeartRateStatistics? {
        if (data.isEmpty()) return null
        
        val heartRates = data.map { it.heartRate }
        val min = heartRates.minOrNull() ?: 0
        val max = heartRates.maxOrNull() ?: 0
        val average = heartRates.average().toFloat()
        val variance = heartRates.map { (it - average) * (it - average) }.average().toFloat()
        
        return HeartRateStatistics(min, max, average, variance)
    }
    
    /**
     * Calculate location statistics
     */
    private fun calculateLocationStatistics(data: List<GpsLocationData>): LocationStatistics? {
        if (data.size < 2) return null
        
        val totalDistance = data.zipWithNext().sumOf { (prev, curr) ->
            calculateDistance(prev.latitude, prev.longitude, curr.latitude, curr.longitude).toDouble()
        }.toFloat()
        
        val averageSpeed = if (data.size > 1) {
            val timeSpan = (data.last().timestamp - data.first().timestamp) / 1000.0f
            if (timeSpan > 0) totalDistance / timeSpan else 0f
        } else 0f
        
        val boundingBox = BoundingBox(
            minLat = data.minOf { it.latitude },
            maxLat = data.maxOf { it.latitude },
            minLon = data.minOf { it.longitude },
            maxLon = data.maxOf { it.longitude }
        )
        
        return LocationStatistics(
            totalDistance = totalDistance,
            averageSpeed = averageSpeed,
            elevationGain = 0f, // Would need altitude data
            boundingBox = boundingBox
        )
    }
    
    /**
     * Calculate movement statistics
     */
    private fun calculateMovementStatistics(deviceData: ProcessedDeviceData): MovementStatistics {
        return MovementStatistics(
            stepCount = deviceData.stepCount,
            caloriesBurned = deviceData.caloriesBurned,
            activeTime = deviceData.activityDurations.values.sum()
        )
    }
    
    /**
     * Calculate battery statistics
     */
    private fun calculateBatteryStatistics(data: List<BatteryData>): BatteryStatistics? {
        if (data.size < 2) return null
        
        val startLevel = data.first().level
        val endLevel = data.last().level
        val timeSpan = (data.last().timestamp - data.first().timestamp) / (1000.0 * 60 * 60) // hours
        val dischargeRate = if (timeSpan > 0) abs((endLevel - startLevel).toFloat()) / timeSpan.toFloat() else 0f
        
        return BatteryStatistics(
            startLevel = startLevel,
            endLevel = endLevel,
            dischargeRate = dischargeRate
        )
    }
    
    /**
     * Calculate distance between two GPS coordinates using Haversine formula
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth's radius in meters
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return r * c
    }
    
    /**
     * Update aggregated data
     */
    private fun updateAggregatedData(deviceAddress: String, deviceData: ProcessedDeviceData) {
        val allData = mutableListOf<TelemetryData>()
        allData.addAll(deviceData.heartRateData)
        allData.addAll(deviceData.gpsLocationData)
        allData.addAll(deviceData.accelerometerData)
        allData.addAll(deviceData.gyroscopeData)
        allData.addAll(deviceData.batteryData)
        allData.addAll(deviceData.environmentalData)
        allData.addAll(deviceData.customSensorData)
        
        if (allData.isNotEmpty()) {
            val statistics = TelemetryStatistics(
                heartRateStats = deviceData.heartRateStats,
                locationStats = deviceData.locationStats,
                movementStats = deviceData.movementStats,
                batteryStats = deviceData.batteryStats
            )
            
            val startTimestamp = allData.minOf { it.timestamp } as Long
            val endTimestamp = allData.maxOf { it.timestamp } as Long
            
            val aggregatedData = AggregatedTelemetryData(
                deviceAddress = deviceAddress,
                startTimestamp = startTimestamp,
                endTimestamp = endTimestamp,
                dataPoints = allData,
                statistics = statistics
            )
            
            val currentAggregated = _aggregatedData.value.toMutableMap()
            currentAggregated[deviceAddress] = aggregatedData
            _aggregatedData.value = currentAggregated
        }
    }
    
    companion object {
        private const val TAG = "TelemetryDataProcessor"
    }
}

/**
 * Data class for storing processed device data
 */
data class ProcessedDeviceData(
    val deviceAddress: String,
    val heartRateData: MutableList<HeartRateData> = mutableListOf(),
    val gpsLocationData: MutableList<GpsLocationData> = mutableListOf(),
    val accelerometerData: MutableList<AccelerometerData> = mutableListOf(),
    val gyroscopeData: MutableList<GyroscopeData> = mutableListOf(),
    val batteryData: MutableList<BatteryData> = mutableListOf(),
    val environmentalData: MutableList<EnvironmentalData> = mutableListOf(),
    val customSensorData: MutableList<CustomSensorData> = mutableListOf(),
    
    // Calculated values
    var totalDistance: Float = 0f,
    var currentSpeed: Float = 0f,
    var speedHistory: MutableList<Float> = mutableListOf(),
    var stepCount: Int = 0,
    var caloriesBurned: Float = 0f,
    var currentActivity: ActivityType = ActivityType.STATIONARY,
    var previousActivity: ActivityType = ActivityType.STATIONARY,
    var lastActivityChangeTime: Long = System.currentTimeMillis(),
    var activityDurations: MutableMap<ActivityType, Long> = mutableMapOf(),
    var isStepDetected: Boolean = false,
    var lastStepTime: Long = 0L,
    
    // Statistics
    var heartRateStats: HeartRateStatistics? = null,
    var locationStats: LocationStatistics? = null,
    var movementStats: MovementStatistics? = null,
    var batteryStats: BatteryStatistics? = null
)

/**
 * Types of physical activities
 */
enum class ActivityType {
    STATIONARY,
    WALKING,
    RUNNING,
    CYCLING,
    HIGH_INTENSITY
}
