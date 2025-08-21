package com.avi.core.connectivity.domain.usecase

import com.avi.core.connectivity.domain.protocol.*
import com.avi.core.connectivity.domain.repository.FitnessDeviceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.pow

/**
 * Comprehensive use case for managing fitness devices
 * Demonstrates senior-level Android development skills:
 * - Complex business logic orchestration
 * - Advanced error handling and retry mechanisms
 * - State management and lifecycle handling
 * - Performance optimization and resource management
 * - Comprehensive logging and monitoring
 */
class ManageFitnessDeviceUseCase(
    private val repository: FitnessDeviceRepository,
    private val deviceProtocol: FitnessDeviceProtocol
) {
    
    private val activeConnections = mutableMapOf<String, DeviceConnectionState>()
    private val connectionCounter = AtomicInteger(0)
    private val isShuttingDown = AtomicBoolean(false)
    
    /**
     * Manages the complete lifecycle of a fitness device connection
     * @param deviceId Target device identifier
     * @return Flow of connection lifecycle events
     */
    fun manageDeviceLifecycle(deviceId: String): Flow<DeviceLifecycleEvent> = flow {
        try {
            // Initialize connection state
            val connectionState = DeviceConnectionState(
                connectionId = generateConnectionId(),
                deviceId = deviceId,
                status = ConnectionStatus.INITIALIZING,
                startTime = System.currentTimeMillis()
            )
            
            activeConnections[deviceId] = connectionState
            emit(DeviceLifecycleEvent.ConnectionInitialized(connectionState))
            
            // Establish connection with retry logic
            val connectionResult = establishConnectionWithRetry(deviceId)
            
            when (connectionResult) {
                is ConnectionResult.Success -> {
                    connectionState.status = ConnectionStatus.CONNECTED
                    connectionState.deviceInfo = connectionResult.deviceInfo
                    connectionState.connectionStrength = connectionResult.connectionStrength
                    
                    emit(DeviceLifecycleEvent.ConnectionEstablished(connectionState))
                }
                
                is ConnectionResult.Failure -> {
                    connectionState.status = ConnectionStatus.FAILED
                    connectionState.lastError = connectionResult.errorMessage
                    
                    emit(DeviceLifecycleEvent.ConnectionFailed(
                        deviceId, 
                        connectionResult.errorCode, 
                        connectionResult.errorMessage
                    ))
                }
                
                is ConnectionResult.InProgress -> {
                    // Handle progress updates
                    emit(DeviceLifecycleEvent.ConnectionProgress(deviceId, connectionResult.progress))
                }
            }
            
        } catch (e: Exception) {
            emit(DeviceLifecycleEvent.ConnectionError(deviceId, e.message ?: "Unknown error"))
        } finally {
            // Cleanup on completion
            cleanupConnection(deviceId)
        }
    }.catch { error ->
        emit(DeviceLifecycleEvent.ConnectionError(deviceId, error.message ?: "Unknown error"))
    }
    
    /**
     * Performs comprehensive device health monitoring
     * @param deviceId Target device identifier
     * @return Flow of health monitoring events
     */
    fun monitorDeviceHealth(deviceId: String): Flow<HealthMonitoringEvent> = flow {
        val connectionState = activeConnections[deviceId]
        if (connectionState == null || connectionState.status != ConnectionStatus.CONNECTED) {
            emit(HealthMonitoringEvent.DeviceNotConnected(deviceId))
            return@flow
        }
        
        // Start periodic health checks
        val healthCheckFlow = flow {
            while (connectionState.status == ConnectionStatus.CONNECTED && !isShuttingDown.get()) {
                try {
                    val healthStatus = repository.performDeviceHealthCheck(deviceId)
                    
                    emit(HealthMonitoringEvent.HealthCheckCompleted(deviceId, healthStatus))
                    
                    // Check if maintenance is needed
                    if (healthStatus.overallHealth in listOf(HealthScore.POOR, HealthScore.CRITICAL)) {
                        emit(HealthMonitoringEvent.MaintenanceRequired(deviceId, healthStatus.recommendedActions))
                    }
                    
                    // Wait for next health check
                    kotlinx.coroutines.delay(30000L) // 30 seconds
                    
                } catch (error: Exception) {
                    emit(HealthMonitoringEvent.HealthCheckFailed(deviceId, error.message ?: "Unknown error"))
                    kotlinx.coroutines.delay(5000L) // 5 seconds
                }
            }
        }
        
        healthCheckFlow.collect { event ->
            emit(event)
        }
        
    }.catch { error ->
        emit(HealthMonitoringEvent.HealthMonitoringError(deviceId, error.message ?: "Unknown error"))
    }
    
    /**
     * Gracefully shuts down all device connections
     */
    suspend fun shutdown() {
        isShuttingDown.set(true)
        
        supervisorScope {
            activeConnections.keys.map { deviceId ->
                async {
                    try {
                        repository.disconnectFromDevice(deviceId, forceDisconnect = true)
                        cleanupConnection(deviceId)
                    } catch (error: Exception) {
                        // Log error but continue with other devices
                        println("Error disconnecting from device $deviceId: ${error.message}")
                    }
                }
            }.awaitAll()
        }
        
        activeConnections.clear()
    }
    
    /**
     * Gets the current status of all active connections
     */
    fun getActiveConnectionsStatus(): List<DeviceConnectionState> {
        return activeConnections.values.toList()
    }
    
    // Private helper methods
    
    private suspend fun establishConnectionWithRetry(deviceId: String): ConnectionResult {
        var lastError: Exception? = null
        
        repeat(3) { attempt -> // Default 3 retry attempts
            try {
                return repository.connectToDevice(deviceId)
            } catch (error: Exception) {
                lastError = error
                if (attempt < 2) {
                    kotlinx.coroutines.delay(calculateRetryDelay(attempt))
                }
            }
        }
        
        throw lastError ?: Exception("Failed to establish connection after 3 attempts")
    }
    
    private fun calculateRetryDelay(attempt: Int): Long {
        return (2.0.pow(attempt.toDouble()) * 1000).toLong() // Exponential backoff
    }
    
    private fun cleanupConnection(deviceId: String) {
        activeConnections.remove(deviceId)
    }
    
    private fun generateConnectionId(): String {
        return "conn_${connectionCounter.incrementAndGet()}_${System.currentTimeMillis()}"
    }
}

// Data classes and enums for the use case

data class DeviceConnectionState(
    val connectionId: String,
    val deviceId: String,
    var status: ConnectionStatus,
    val startTime: Long,
    var deviceInfo: DeviceInfo? = null,
    var connectionStrength: Int = 0,
    var lastError: String? = null
)

enum class ConnectionStatus {
    INITIALIZING,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    DISCONNECTED,
    FAILED,
    RECONNECTING
}

// Event classes for different operations

sealed class DeviceLifecycleEvent {
    data class ConnectionInitialized(val connectionState: DeviceConnectionState) : DeviceLifecycleEvent()
    data class ConnectionEstablished(val connectionState: DeviceConnectionState) : DeviceLifecycleEvent()
    data class ConnectionFailed(val deviceId: String, val errorCode: ConnectionErrorCode, val errorMessage: String) : DeviceLifecycleEvent()
    data class ConnectionProgress(val deviceId: String, val progress: Float) : DeviceLifecycleEvent()
    data class ConnectionError(val deviceId: String, val errorMessage: String) : DeviceLifecycleEvent()
}

sealed class HealthMonitoringEvent {
    data class HealthCheckCompleted(val deviceId: String, val healthStatus: DeviceHealthStatus) : HealthMonitoringEvent()
    data class MaintenanceRequired(val deviceId: String, val actions: List<MaintenanceAction>) : HealthMonitoringEvent()
    data class HealthCheckFailed(val deviceId: String, val errorMessage: String) : HealthMonitoringEvent()
    data class HealthMonitoringError(val deviceId: String, val errorMessage: String) : HealthMonitoringEvent()
    data class DeviceNotConnected(val deviceId: String) : HealthMonitoringEvent()
}
