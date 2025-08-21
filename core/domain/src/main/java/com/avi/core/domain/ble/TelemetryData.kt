package com.avi.core.domain.ble

/**
 * Base class for all telemetry data from embedded devices
 */
sealed class TelemetryData {
    abstract val timestamp: Long
    abstract val deviceAddress: String
    abstract val dataType: TelemetryDataType
}

/**
 * Types of telemetry data that can be received
 */
enum class TelemetryDataType {
    HEART_RATE,
    GPS_LOCATION,
    ACCELEROMETER,
    GYROSCOPE,
    BATTERY_LEVEL,
    TEMPERATURE,
    HUMIDITY,
    PRESSURE,
    CUSTOM_SENSOR
}

/**
 * Heart rate telemetry data
 */
data class HeartRateData(
    override val timestamp: Long,
    override val deviceAddress: String,
    val heartRate: Int, // BPM
    val confidence: Float = 1.0f, // 0.0 to 1.0
    val energyExpended: Int? = null, // kJ
    override val dataType: TelemetryDataType = TelemetryDataType.HEART_RATE
) : TelemetryData()

/**
 * GPS location telemetry data
 */
data class GpsLocationData(
    override val timestamp: Long,
    override val deviceAddress: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val speed: Float? = null, // m/s
    override val dataType: TelemetryDataType = TelemetryDataType.GPS_LOCATION
) : TelemetryData()

/**
 * Accelerometer telemetry data
 */
data class AccelerometerData(
    override val timestamp: Long,
    override val deviceAddress: String,
    val x: Float, // m/s²
    val y: Float, // m/s²
    val z: Float, // m/s²
    val magnitude: Float = kotlin.math.sqrt(x * x + y * y + z * z),
    override val dataType: TelemetryDataType = TelemetryDataType.ACCELEROMETER
) : TelemetryData()

/**
 * Gyroscope telemetry data
 */
data class GyroscopeData(
    override val timestamp: Long,
    override val deviceAddress: String,
    val x: Float, // rad/s
    val y: Float, // rad/s
    val z: Float, // rad/s
    val magnitude: Float = kotlin.math.sqrt(x * x + y * y + z * z),
    override val dataType: TelemetryDataType = TelemetryDataType.GYROSCOPE
) : TelemetryData()

/**
 * Battery level telemetry data
 */
data class BatteryData(
    override val timestamp: Long,
    override val deviceAddress: String,
    val level: Int, // 0-100%
    val isCharging: Boolean = false,
    val voltage: Float? = null, // V
    val temperature: Float? = null, // °C
    override val dataType: TelemetryDataType = TelemetryDataType.BATTERY_LEVEL
) : TelemetryData()

/**
 * Environmental sensor telemetry data
 */
data class EnvironmentalData(
    override val timestamp: Long,
    override val deviceAddress: String,
    val temperature: Float? = null, // °C
    val humidity: Float? = null, // %
    val pressure: Float? = null, // hPa
    override val dataType: TelemetryDataType = TelemetryDataType.TEMPERATURE
) : TelemetryData()

/**
 * Custom sensor telemetry data for device-specific sensors
 */
data class CustomSensorData(
    override val timestamp: Long,
    override val deviceAddress: String,
    val sensorId: String,
    val values: Map<String, Float>,
    val unit: String = "",
    override val dataType: TelemetryDataType = TelemetryDataType.CUSTOM_SENSOR
) : TelemetryData()

/**
 * Aggregated telemetry data for efficient processing
 */
data class AggregatedTelemetryData(
    val deviceAddress: String,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val dataPoints: List<TelemetryData>,
    val statistics: TelemetryStatistics
)

/**
 * Statistical summary of telemetry data
 */
data class TelemetryStatistics(
    val heartRateStats: HeartRateStatistics? = null,
    val locationStats: LocationStatistics? = null,
    val movementStats: MovementStatistics? = null,
    val batteryStats: BatteryStatistics? = null
)

data class HeartRateStatistics(
    val min: Int,
    val max: Int,
    val average: Float,
    val variance: Float
)

data class LocationStatistics(
    val totalDistance: Float, // meters
    val averageSpeed: Float, // m/s
    val elevationGain: Float, // meters
    val boundingBox: BoundingBox
)

data class MovementStatistics(
    val stepCount: Int,
    val caloriesBurned: Float,
    val activeTime: Long // milliseconds
)

data class BatteryStatistics(
    val startLevel: Int,
    val endLevel: Int,
    val dischargeRate: Float // %/hour
)

data class BoundingBox(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
)
