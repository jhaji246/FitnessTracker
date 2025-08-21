package com.avi.core.connectivity.domain.repository

import com.avi.core.connectivity.domain.protocol.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for fitness device management
 * Demonstrates advanced Android architecture patterns:
 * - Repository pattern with offline-first approach
 * - Comprehensive error handling and retry mechanisms
 * - Caching strategies and data synchronization
 * - Background processing and conflict resolution
 */
interface FitnessDeviceRepository {
    
    /**
     * Discovers nearby fitness devices
     * @param scanDuration Duration of the scan in milliseconds
     * @param filterCriteria Optional filtering criteria
     * @return Flow of discovered devices
     */
    suspend fun discoverDevices(
        scanDuration: Long = 10000L,
        filterCriteria: DeviceFilterCriteria? = null
    ): Flow<DeviceDiscoveryResult>
    
    /**
     * Connects to a fitness device with advanced connection management
     * @param deviceId Target device identifier
     * @param connectionOptions Connection configuration options
     * @return Connection result with detailed status
     */
    suspend fun connectToDevice(
        deviceId: String,
        connectionOptions: ConnectionOptions = ConnectionOptions()
    ): ConnectionResult
    
    /**
     * Disconnects from a device and manages cleanup
     * @param deviceId Target device identifier
     * @param forceDisconnect Whether to force immediate disconnection
     */
    suspend fun disconnectFromDevice(
        deviceId: String,
        forceDisconnect: Boolean = false
    )
    
    /**
     * Streams real-time data from connected devices
     * @param deviceId Source device identifier
     * @param dataTypes Types of sensor data to stream
     * @return Flow of sensor data with metadata
     */
    suspend fun streamDeviceData(
        deviceId: String,
        dataTypes: List<SensorType> = emptyList()
    ): Flow<DeviceDataStream>
    
    /**
     * Sends commands to connected devices
     * @param deviceId Target device identifier
     * @param command Command to execute
     * @return Command execution result
     */
    suspend fun sendDeviceCommand(
        deviceId: String,
        command: DeviceCommand
    ): CommandResult
    
    /**
     * Manages device firmware updates
     * @param deviceId Target device identifier
     * @param firmwareUpdate Firmware update information
     * @return Update progress flow
     */
    suspend fun updateDeviceFirmware(
        deviceId: String,
        firmwareUpdate: FirmwareUpdate
    ): Flow<FirmwareUpdateProgress>
    
    /**
     * Performs comprehensive device health monitoring
     * @param deviceId Target device identifier
     * @param healthCheckOptions Health check configuration
     * @return Device health status
     */
    suspend fun performDeviceHealthCheck(
        deviceId: String,
        healthCheckOptions: HealthCheckOptions = HealthCheckOptions()
    ): DeviceHealthStatus
    
    /**
     * Manages device data synchronization
     * @param deviceId Target device identifier
     * @param syncOptions Synchronization configuration
     * @return Sync progress and results
     */
    suspend fun syncDeviceData(
        deviceId: String,
        syncOptions: DataSyncOptions = DataSyncOptions()
    ): Flow<DataSyncProgress>
    
    /**
     * Retrieves cached device data for offline access
     * @param deviceId Target device identifier
     * @param dataType Type of data to retrieve
     * @param timeRange Time range for data retrieval
     * @return Cached data with metadata
     */
    suspend fun getCachedDeviceData(
        deviceId: String,
        dataType: SensorType,
        timeRange: TimeRange
    ): List<CachedSensorData>
    
    /**
     * Manages device pairing and authentication
     * @param deviceId Target device identifier
     * @param authenticationMethod Authentication method to use
     * @return Authentication result
     */
    suspend fun authenticateDevice(
        deviceId: String,
        authenticationMethod: AuthenticationMethod
    ): AuthenticationResult
    
    /**
     * Configures device settings and preferences
     * @param deviceId Target device identifier
     * @param configuration Device configuration settings
     * @return Configuration result
     */
    suspend fun configureDevice(
        deviceId: String,
        configuration: DeviceConfiguration
    ): ConfigurationResult
    
    /**
     * Monitors device connection status and health
     * @param deviceId Target device identifier
     * @return Connection monitoring flow
     */
    fun monitorDeviceConnection(deviceId: String): Flow<DeviceConnectionStatus>
    
    /**
     * Manages device data backup and restoration
     * @param deviceId Target device identifier
     * @param backupOptions Backup configuration options
     * @return Backup progress and results
     */
    suspend fun backupDeviceData(
        deviceId: String,
        backupOptions: DataBackupOptions = DataBackupOptions()
    ): Flow<DataBackupProgress>
}

/**
 * Device discovery result
 */
sealed class DeviceDiscoveryResult {
    data class DeviceFound(
        val device: DeviceInfo,
        val signalStrength: Int,
        val isConnectable: Boolean
    ) : DeviceDiscoveryResult()
    
    data class DiscoveryComplete(
        val totalDevicesFound: Int,
        val scanDuration: Long
    ) : DeviceDiscoveryResult()
    
    data class DiscoveryError(
        val errorCode: DiscoveryErrorCode,
        val errorMessage: String
    ) : DeviceDiscoveryResult()
}

/**
 * Discovery error codes
 */
enum class DiscoveryErrorCode {
    BLUETOOTH_DISABLED,
    LOCATION_PERMISSION_DENIED,
    SCAN_FAILED,
    TIMEOUT,
    UNKNOWN_ERROR
}

/**
 * Device filter criteria for discovery
 */
data class DeviceFilterCriteria(
    val manufacturer: String? = null,
    val supportedFeatures: List<DeviceFeature> = emptyList(),
    val minSignalStrength: Int = -100,
    val maxDistance: Float? = null,
    val deviceTypes: List<String> = emptyList()
)

/**
 * Connection options for device connection
 */
data class ConnectionOptions(
    val timeout: Long = 30000L,
    val retryAttempts: Int = 3,
    val secureConnection: Boolean = true,
    val autoReconnect: Boolean = true,
    val connectionPriority: ConnectionPriority = ConnectionPriority.BALANCED
)

/**
 * Connection priority levels
 */
enum class ConnectionPriority {
    LOW_POWER,
    BALANCED,
    HIGH_PERFORMANCE
}

/**
 * Device data stream with metadata
 */
data class DeviceDataStream(
    val deviceId: String,
    val timestamp: Long,
    val dataType: SensorType,
    val sensorData: Map<String, Float>,
    val accuracy: Int,
    val batteryLevel: Int,
    val connectionQuality: ConnectionQuality
)

/**
 * Connection quality indicators
 */
enum class ConnectionQuality {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    UNSTABLE
}

/**
 * Firmware update information
 */
data class FirmwareUpdate(
    val version: String,
    val releaseNotes: String,
    val firmwareData: ByteArray,
    val checksum: String,
    val isMandatory: Boolean = false,
    val estimatedDuration: Long? = null
)

/**
 * Data synchronization options
 */
data class DataSyncOptions(
    val syncMode: SyncMode = SyncMode.INCREMENTAL,
    val conflictResolution: ConflictResolution = ConflictResolution.SERVER_WINS,
    val maxRetries: Int = 3,
    val batchSize: Int = 100
)

/**
 * Synchronization modes
 */
enum class SyncMode {
    FULL,
    INCREMENTAL,
    SELECTIVE,
    BACKGROUND
}

/**
 * Conflict resolution strategies
 */
enum class ConflictResolution {
    SERVER_WINS,
    CLIENT_WINS,
    MERGE,
    MANUAL
}

/**
 * Data synchronization progress
 */
data class DataSyncProgress(
    val stage: SyncStage,
    val progress: Float,
    val currentStep: String,
    val totalSteps: Int,
    val recordsProcessed: Int,
    val recordsTotal: Int,
    val estimatedTimeRemaining: Long?,
    val conflicts: List<DataConflict> = emptyList()
)

/**
 * Synchronization stages
 */
enum class SyncStage {
    PREPARING,
    VALIDATING,
    TRANSFERRING,
    PROCESSING,
    RESOLVING_CONFLICTS,
    COMPLETING,
    COMPLETED,
    FAILED
}

/**
 * Data conflict information
 */
data class DataConflict(
    val recordId: String,
    val conflictType: ConflictType,
    val serverValue: String?,
    val clientValue: String?,
    val timestamp: Long
)

/**
 * Conflict types
 */
enum class ConflictType {
    VALUE_MISMATCH,
    TIMESTAMP_CONFLICT,
    DELETION_CONFLICT,
    SCHEMA_MISMATCH
}

/**
 * Cached sensor data
 */
data class CachedSensorData(
    val id: String,
    val deviceId: String,
    val sensorType: SensorType,
    val timestamp: Long,
    val data: Map<String, Float>,
    val accuracy: Int,
    val isSynced: Boolean,
    val lastModified: Long
)

/**
 * Time range for data queries
 */
data class TimeRange(
    val startTime: Long,
    val endTime: Long
)

/**
 * Authentication methods
 */
enum class AuthenticationMethod {
    PIN_CODE,
    BIOMETRIC,
    QR_CODE,
    NFC,
    MANUAL_PAIRING
}

/**
 * Authentication result
 */
sealed class AuthenticationResult {
    data class Success(
        val deviceId: String,
        val authenticationToken: String?,
        val permissions: List<DevicePermission>
    ) : AuthenticationResult()
    
    data class Failure(
        val errorCode: AuthenticationErrorCode,
        val errorMessage: String,
        val retryable: Boolean = false
    ) : AuthenticationResult()
}

/**
 * Authentication error codes
 */
enum class AuthenticationErrorCode {
    INVALID_CREDENTIALS,
    DEVICE_LOCKED,
    TOO_MANY_ATTEMPTS,
    AUTHENTICATION_TIMEOUT,
    UNSUPPORTED_METHOD,
    PERMISSION_DENIED
}

/**
 * Device permissions
 */
enum class DevicePermission {
    READ_DATA,
    WRITE_DATA,
    CONFIGURE_DEVICE,
    UPDATE_FIRMWARE,
    MANAGE_CONNECTION,
    ACCESS_BIOMETRICS
}

/**
 * Device configuration settings
 */
data class DeviceConfiguration(
    val sensorConfigurations: Map<SensorType, SensorConfiguration> = emptyMap(),
    val trackingSettings: TrackingSettings = TrackingSettings(),
    val powerSettings: PowerSettings = PowerSettings(),
    val notificationSettings: NotificationSettings = NotificationSettings()
)

/**
 * Sensor configuration
 */
data class SensorConfiguration(
    val enabled: Boolean = true,
    val samplingRate: Int = 100,
    val sensitivity: Float = 1.0f,
    val calibrationData: Map<String, Float> = emptyMap()
)

/**
 * Tracking settings
 */
data class TrackingSettings(
    val mode: TrackingMode = TrackingMode.CONTINUOUS,
    val interval: Long = 1000L,
    val autoStart: Boolean = false,
    val autoStop: Boolean = false
)

/**
 * Power settings
 */
data class PowerSettings(
    val powerMode: PowerMode = PowerMode.BALANCED,
    val sleepTimeout: Long = 300000L,
    val lowPowerThreshold: Int = 20
)

/**
 * Power modes
 */
enum class PowerMode {
    HIGH_PERFORMANCE,
    BALANCED,
    POWER_SAVING,
    ULTRA_POWER_SAVING
}

/**
 * Notification settings
 */
data class NotificationSettings(
    val enabled: Boolean = true,
    val vibration: Boolean = true,
    val sound: Boolean = false,
    val priority: NotificationPriority = NotificationPriority.NORMAL
)

/**
 * Notification priority levels
 */
enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * Configuration result
 */
sealed class ConfigurationResult {
    data class Success(
        val deviceId: String,
        val appliedSettings: List<String>,
        val requiresRestart: Boolean = false
    ) : ConfigurationResult()
    
    data class Failure(
        val errorCode: String,
        val errorMessage: String,
        val failedSettings: List<String> = emptyList()
    ) : ConfigurationResult()
}

/**
 * Device connection status
 */
data class DeviceConnectionStatus(
    val deviceId: String,
    val isConnected: Boolean,
    val connectionType: ConnectionType?,
    val signalStrength: Int?,
    val batteryLevel: Int?,
    val lastSeen: Long,
    val connectionQuality: ConnectionQuality
)

/**
 * Connection types
 */
enum class ConnectionType {
    BLE,
    CLASSIC_BLUETOOTH,
    WIFI_DIRECT,
    USB
}

/**
 * Data backup options
 */
data class DataBackupOptions(
    val backupType: BackupType = BackupType.FULL,
    val includeSettings: Boolean = true,
    val includeUserData: Boolean = true,
    val compression: Boolean = true,
    val encryption: Boolean = true
)

/**
 * Backup types
 */
enum class BackupType {
    FULL,
    INCREMENTAL,
    SELECTIVE,
    SETTINGS_ONLY
}

/**
 * Data backup progress
 */
data class DataBackupProgress(
    val stage: BackupStage,
    val progress: Float,
    val currentStep: String,
    val totalSteps: Int,
    val dataSize: Long,
    val estimatedTimeRemaining: Long?,
    val backupLocation: String? = null
)

/**
 * Backup stages
 */
enum class BackupStage {
    PREPARING,
    VALIDATING,
    BACKING_UP,
    COMPRESSING,
    ENCRYPTING,
    UPLOADING,
    COMPLETING,
    COMPLETED,
    FAILED
}

// Missing classes that are referenced in the repository

/**
 * Health check options for device health monitoring
 */
data class HealthCheckOptions(
    val includeDetailedMetrics: Boolean = true,
    val checkBatteryHealth: Boolean = true,
    val checkSensorHealth: Boolean = true,
    val checkConnectionHealth: Boolean = true,
    val timeout: Long = 30000L
)

/**
 * Health monitoring options for continuous monitoring
 */
data class HealthMonitoringOptions(
    val healthCheckInterval: Long = 30000L, // 30 seconds
    val retryInterval: Long = 5000L, // 5 seconds
    val includeDetailedMetrics: Boolean = true,
    val checkBatteryHealth: Boolean = true,
    val checkSensorHealth: Boolean = true,
    val checkConnectionHealth: Boolean = true
)

/**
 * Health monitoring events
 */
sealed class HealthMonitoringEvent {
    data class HealthCheckCompleted(val deviceId: String, val healthStatus: DeviceHealthStatus) : HealthMonitoringEvent()
    data class MaintenanceRequired(val deviceId: String, val actions: List<MaintenanceAction>) : HealthMonitoringEvent()
    data class HealthCheckFailed(val deviceId: String, val errorMessage: String) : HealthMonitoringEvent()
    data class HealthMonitoringError(val deviceId: String, val errorMessage: String) : HealthMonitoringEvent()
    data class DeviceNotConnected(val deviceId: String) : HealthMonitoringEvent()
}

/**
 * Firmware update events
 */
sealed class FirmwareUpdateEvent {
    data class UpdateStarted(val deviceId: String, val firmwareUpdate: FirmwareUpdate) : FirmwareUpdateEvent()
    data class UpdateProgress(val deviceId: String, val progress: FirmwareUpdateProgress) : FirmwareUpdateEvent()
    data class UpdateCompleted(val deviceId: String, val version: String) : FirmwareUpdateEvent()
    data class UpdateFailed(val deviceId: String, val errorMessage: String) : FirmwareUpdateEvent()
    data class UpdateAborted(val deviceId: String, val reason: String) : FirmwareUpdateEvent()
    data class ValidationFailed(val deviceId: String, val errors: List<String>) : FirmwareUpdateEvent()
    data class DeviceNotConnected(val deviceId: String) : FirmwareUpdateEvent()
    data class UpdateError(val deviceId: String, val errorMessage: String) : FirmwareUpdateEvent()
}

/**
 * Data synchronization events
 */
sealed class DataSyncEvent {
    data class SyncStarted(val deviceId: String) : DataSyncEvent()
    data class SyncProgress(val deviceId: String, val progress: DataSyncProgress) : DataSyncEvent()
    data class SyncCompleted(val deviceId: String, val recordsProcessed: Int) : DataSyncEvent()
    data class SyncFailed(val deviceId: String, val errorMessage: String) : DataSyncEvent()
    data class ConflictsResolved(val deviceId: String, val resolvedConflicts: List<ResolvedConflict>) : DataSyncEvent()
    data class DeviceNotConnected(val deviceId: String) : DataSyncEvent()
    data class SyncError(val deviceId: String, val errorMessage: String) : DataSyncEvent()
}

/**
 * Resolved conflict information
 */
data class ResolvedConflict(
    val conflictId: String,
    val originalConflict: DataConflict,
    val resolvedValue: String?,
    val resolutionStrategy: ConflictResolution
)
