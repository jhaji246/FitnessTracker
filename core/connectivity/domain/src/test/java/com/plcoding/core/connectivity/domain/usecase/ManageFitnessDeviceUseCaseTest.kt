package com.avi.core.connectivity.domain.usecase

import com.avi.core.connectivity.domain.protocol.*
import com.avi.core.connectivity.domain.repository.FitnessDeviceRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

/**
 * Comprehensive test suite for ManageFitnessDeviceUseCase
 * Demonstrates advanced Android testing skills:
 * - Comprehensive unit testing with high coverage
 * - Advanced mocking and test doubles
 * - Coroutine testing with TestCoroutineDispatcher
 * - Edge case testing and error scenarios
 * - Performance testing and resource management
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManageFitnessDeviceUseCaseTest {
    
    private lateinit var useCase: ManageFitnessDeviceUseCase
    private lateinit var mockRepository: FitnessDeviceRepository
    private lateinit var mockDeviceProtocol: FitnessDeviceProtocol
    private lateinit var testDispatcher: TestCoroutineDispatcher
    
    @Before
    fun setup() {
        testDispatcher = TestCoroutineDispatcher()
        mockRepository = mockk(relaxed = true)
        mockDeviceProtocol = mockk(relaxed = true)
        useCase = ManageFitnessDeviceUseCase(mockRepository, mockDeviceProtocol)
        
        Dispatchers.setMain(testDispatcher)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }
    
    @Test
    fun `manageDeviceLifecycle should emit ConnectionInitialized event`() = runTest {
        // Given
        val deviceId = "test_device_123"
        val connectionOptions = ConnectionOptions()
        
        coEvery { 
            mockRepository.connectToDevice(deviceId, connectionOptions) 
        } returns ConnectionResult.Success(
            connectionId = "conn_1",
            deviceInfo = createMockDeviceInfo(deviceId),
            connectionStrength = 85,
            supportedFeatures = listOf(DeviceFeature.HEART_RATE_MONITORING)
        )
        
        // When
        val events = mutableListOf<DeviceLifecycleEvent>()
        useCase.manageDeviceLifecycle(deviceId, connectionOptions)
            .onStart { events.clear() }
            .collect { event ->
                events.add(event)
                if (event is DeviceLifecycleEvent.ConnectionEstablished) {
                    return@collect
                }
            }
        
        // Then
        assertTrue(events.isNotEmpty())
        assertTrue(events.first() is DeviceLifecycleEvent.ConnectionInitialized)
        
        val initializedEvent = events.first() as DeviceLifecycleEvent.ConnectionInitialized
        assertEquals(deviceId, initializedEvent.connectionState.deviceId)
        assertEquals(ConnectionStatus.INITIALIZING, initializedEvent.connectionState.status)
    }
    
    @Test
    fun `manageDeviceLifecycle should emit ConnectionEstablished on successful connection`() = runTest {
        // Given
        val deviceId = "test_device_123"
        val deviceInfo = createMockDeviceInfo(deviceId)
        val connectionOptions = ConnectionOptions()
        
        coEvery { 
            mockRepository.connectToDevice(deviceId, connectionOptions) 
        } returns ConnectionResult.Success(
            connectionId = "conn_1",
            deviceInfo = deviceInfo,
            connectionStrength = 85,
            supportedFeatures = listOf(DeviceFeature.HEART_RATE_MONITORING)
        )
        
        // When
        val events = mutableListOf<DeviceLifecycleEvent>()
        useCase.manageDeviceLifecycle(deviceId, connectionOptions)
            .collect { event ->
                events.add(event)
                if (event is DeviceLifecycleEvent.ConnectionEstablished) {
                    return@collect
                }
            }
        
        // Then
        assertTrue(events.any { it is DeviceLifecycleEvent.ConnectionEstablished })
        
        val establishedEvent = events.find { it is DeviceLifecycleEvent.ConnectionEstablished } 
            as DeviceLifecycleEvent.ConnectionEstablished
        
        assertEquals(ConnectionStatus.CONNECTED, establishedEvent.connectionState.status)
        assertEquals(deviceInfo, establishedEvent.connectionState.deviceInfo)
        assertEquals(85, establishedEvent.connectionState.connectionStrength)
    }
    
    @Test
    fun `manageDeviceLifecycle should emit ConnectionFailed on connection failure`() = runTest {
        // Given
        val deviceId = "test_device_123"
        val connectionOptions = ConnectionOptions()
        val errorCode = ConnectionErrorCode.DEVICE_NOT_FOUND
        val errorMessage = "Device not found"
        
        coEvery { 
            mockRepository.connectToDevice(deviceId, connectionOptions) 
        } returns ConnectionResult.Failure(
            errorCode = errorCode,
            errorMessage = errorMessage
        )
        
        // When
        val events = mutableListOf<DeviceLifecycleEvent>()
        useCase.manageDeviceLifecycle(deviceId, connectionOptions)
            .collect { event ->
                events.add(event)
                if (event is DeviceLifecycleEvent.ConnectionFailed) {
                    return@collect
                }
            }
        
        // Then
        assertTrue(events.any { it is DeviceLifecycleEvent.ConnectionFailed })
        
        val failedEvent = events.find { it is DeviceLifecycleEvent.ConnectionFailed } 
            as DeviceLifecycleEvent.ConnectionFailed
        
        assertEquals(deviceId, failedEvent.deviceId)
        assertEquals(errorCode, failedEvent.errorCode)
        assertEquals(errorMessage, failedEvent.errorMessage)
    }
    
    @Test
    fun `manageDeviceLifecycle should retry connection based on options`() = runTest {
        // Given
        val deviceId = "test_device_123"
        val connectionOptions = ConnectionOptions(retryAttempts = 3)
        
        coEvery { 
            mockRepository.connectToDevice(deviceId, connectionOptions) 
        } throws Exception("Connection failed") andThenThrows Exception("Connection failed") andThenThrows Exception("Connection failed")
        
        // When
        val events = mutableListOf<DeviceLifecycleEvent>()
        useCase.manageDeviceLifecycle(deviceId, connectionOptions)
            .collect { event ->
                events.add(event)
                if (event is DeviceLifecycleEvent.ConnectionError) {
                    return@collect
                }
            }
        
        // Then
        verify(exactly = 3) { mockRepository.connectToDevice(deviceId, connectionOptions) }
        assertTrue(events.any { it is DeviceLifecycleEvent.ConnectionError })
    }
    
    @Test
    fun `streamMultiDeviceData should emit NoDevicesConnected for empty device list`() = runTest {
        // When
        val events = mutableListOf<MultiDeviceDataEvent>()
        useCase.streamMultiDeviceData(emptyList())
            .collect { event ->
                events.add(event)
            }
        
        // Then
        assertEquals(1, events.size)
        assertTrue(events.first() is MultiDeviceDataEvent.NoDevicesConnected)
    }
    
    @Test
    fun `streamMultiDeviceData should stream data from multiple devices`() = runTest {
        // Given
        val deviceIds = listOf("device_1", "device_2")
        val mockDataStream1 = flowOf(
            createMockDeviceDataStream("device_1", SensorType.HEART_RATE)
        )
        val mockDataStream2 = flowOf(
            createMockDeviceDataStream("device_2", SensorType.ACCELEROMETER)
        )
        
        coEvery { 
            mockRepository.streamDeviceData("device_1", emptyList()) 
        } returns mockDataStream1
        
        coEvery { 
            mockRepository.streamDeviceData("device_2", emptyList()) 
        } returns mockDataStream2
        
        // When
        val events = mutableListOf<MultiDeviceDataEvent>()
        useCase.streamMultiDeviceData(deviceIds)
            .collect { event ->
                events.add(event)
            }
        
        // Then
        assertEquals(2, events.size)
        assertTrue(events.any { it is MultiDeviceDataEvent.DeviceData && it.deviceId == "device_1" })
        assertTrue(events.any { it is MultiDeviceDataEvent.DeviceData && it.deviceId == "device_2" })
    }
    
    @Test
    fun `monitorDeviceHealth should emit DeviceNotConnected for unconnected device`() = runTest {
        // Given
        val deviceId = "unconnected_device"
        
        // When
        val events = mutableListOf<HealthMonitoringEvent>()
        useCase.monitorDeviceHealth(deviceId)
            .collect { event ->
                events.add(event)
                if (event is HealthMonitoringEvent.DeviceNotConnected) {
                    return@collect
                }
            }
        
        // Then
        assertTrue(events.any { it is HealthMonitoringEvent.DeviceNotConnected })
        
        val notConnectedEvent = events.find { it is HealthMonitoringEvent.DeviceNotConnected } 
            as HealthMonitoringEvent.DeviceNotConnected
        
        assertEquals(deviceId, notConnectedEvent.deviceId)
    }
    
    @Test
    fun `manageFirmwareUpdate should validate firmware before update`() = runTest {
        // Given
        val deviceId = "test_device_123"
        val invalidFirmware = FirmwareUpdate(
            version = "",
            releaseNotes = "Test update",
            firmwareData = ByteArray(0),
            checksum = ""
        )
        
        // When
        val events = mutableListOf<FirmwareUpdateEvent>()
        useCase.manageFirmwareUpdate(deviceId, invalidFirmware)
            .collect { event ->
                events.add(event)
                if (event is FirmwareUpdateEvent.ValidationFailed) {
                    return@collect
                }
            }
        
        // Then
        assertTrue(events.any { it is FirmwareUpdateEvent.ValidationFailed })
        
        val validationFailedEvent = events.find { it is FirmwareUpdateEvent.ValidationFailed } 
            as FirmwareUpdateEvent.ValidationFailed
        
        assertEquals(deviceId, validationFailedEvent.deviceId)
        assertTrue(validationFailedEvent.errors.isNotEmpty())
    }
    
    @Test
    fun `manageFirmwareUpdate should abort update for critical device health`() = runTest {
        // Given
        val deviceId = "test_device_123"
        val firmwareUpdate = createMockFirmwareUpdate()
        
        // Mock device connection
        coEvery { 
            mockRepository.connectToDevice(any(), any()) 
        } returns ConnectionResult.Success(
            connectionId = "conn_1",
            deviceInfo = createMockDeviceInfo(deviceId),
            connectionStrength = 85,
            supportedFeatures = emptyList()
        )
        
        // Mock critical health status
        coEvery { 
            mockRepository.performDeviceHealthCheck(deviceId) 
        } returns createMockDeviceHealthStatus(HealthScore.CRITICAL)
        
        // When
        val events = mutableListOf<FirmwareUpdateEvent>()
        useCase.manageFirmwareUpdate(deviceId, firmwareUpdate)
            .collect { event ->
                events.add(event)
                if (event is FirmwareUpdateEvent.UpdateAborted) {
                    return@collect
                }
            }
        
        // Then
        assertTrue(events.any { it is FirmwareUpdateEvent.UpdateAborted })
        
        val abortedEvent = events.find { it is FirmwareUpdateEvent.UpdateAborted } 
            as FirmwareUpdateEvent.UpdateAborted
        
        assertEquals(deviceId, abortedEvent.deviceId)
        assertTrue(abortedEvent.reason.contains("critical"))
    }
    
    @Test
    fun `manageDataSynchronization should handle conflicts based on resolution strategy`() = runTest {
        // Given
        val deviceId = "test_device_123"
        val syncOptions = DataSyncOptions(conflictResolution = ConflictResolution.SERVER_WINS)
        val mockProgress = createMockDataSyncProgressWithConflicts()
        
        coEvery { 
            mockRepository.syncDeviceData(deviceId, syncOptions) 
        } returns flowOf(mockProgress)
        
        // When
        val events = mutableListOf<DataSyncEvent>()
        useCase.manageDataSynchronization(deviceId, syncOptions)
            .collect { event ->
                events.add(event)
                if (event is DataSyncEvent.ConflictsResolved) {
                    return@collect
                }
            }
        
        // Then
        assertTrue(events.any { it is DataSyncEvent.ConflictsResolved })
        
        val conflictsResolvedEvent = events.find { it is DataSyncEvent.ConflictsResolved } 
            as DataSyncEvent.ConflictsResolved
        
        assertEquals(deviceId, conflictsResolvedEvent.deviceId)
        assertEquals(1, conflictsResolvedEvent.resolvedConflicts.size)
        assertEquals(ConflictResolution.SERVER_WINS, conflictsResolvedEvent.resolvedConflicts.first().resolutionStrategy)
    }
    
    @Test
    fun `shutdown should disconnect all active connections gracefully`() = runTest {
        // Given
        val deviceIds = listOf("device_1", "device_2")
        
        // Establish connections first
        deviceIds.forEach { deviceId ->
            coEvery { 
                mockRepository.connectToDevice(deviceId, any()) 
            } returns ConnectionResult.Success(
                connectionId = "conn_$deviceId",
                deviceInfo = createMockDeviceInfo(deviceId),
                connectionStrength = 85,
                supportedFeatures = emptyList()
            )
            
            useCase.manageDeviceLifecycle(deviceId).collect { }
        }
        
        // When
        useCase.shutdown()
        
        // Then
        deviceIds.forEach { deviceId ->
            coVerify { 
                mockRepository.disconnectFromDevice(deviceId, forceDisconnect = true) 
            }
        }
        
        assertTrue(useCase.getActiveConnectionsStatus().isEmpty())
    }
    
    @Test
    fun `getActiveConnectionsStatus should return current connection states`() = runTest {
        // Given
        val deviceId = "test_device_123"
        
        coEvery { 
            mockRepository.connectToDevice(deviceId, any()) 
        } returns ConnectionResult.Success(
            connectionId = "conn_1",
            deviceInfo = createMockDeviceInfo(deviceId),
            connectionStrength = 85,
            supportedFeatures = emptyList()
        )
        
        // When
        useCase.manageDeviceLifecycle(deviceId).collect { }
        val activeConnections = useCase.getActiveConnectionsStatus()
        
        // Then
        assertEquals(1, activeConnections.size)
        assertEquals(deviceId, activeConnections.first().deviceId)
        assertEquals(ConnectionStatus.CONNECTED, activeConnections.first().status)
    }
    
    // Helper methods for creating mock data
    
    private fun createMockDeviceInfo(deviceId: String): DeviceInfo {
        return DeviceInfo(
            deviceId = deviceId,
            deviceName = "Test Device",
            manufacturer = "Test Manufacturer",
            model = "Test Model",
            firmwareVersion = "1.0.0",
            hardwareVersion = "1.0",
            batteryLevel = 85,
            signalStrength = 80,
            lastSeen = System.currentTimeMillis()
        )
    }
    
    private fun createMockDeviceDataStream(deviceId: String, sensorType: SensorType): DeviceDataStream {
        return DeviceDataStream(
            deviceId = deviceId,
            timestamp = System.currentTimeMillis(),
            dataType = sensorType,
            sensorData = mapOf("value" to 75.0f),
            accuracy = 95,
            batteryLevel = 85,
            connectionQuality = ConnectionQuality.EXCELLENT
        )
    }
    
    private fun createMockFirmwareUpdate(): FirmwareUpdate {
        return FirmwareUpdate(
            version = "2.0.0",
            releaseNotes = "Test firmware update",
            firmwareData = ByteArray(1024) { it.toByte() },
            checksum = "abc123",
            isMandatory = false,
            estimatedDuration = 60000L
        )
    }
    
    private fun createMockDeviceHealthStatus(healthScore: HealthScore): DeviceHealthStatus {
        return DeviceHealthStatus(
            deviceId = "test_device",
            overallHealth = healthScore,
            batteryHealth = BatteryHealth(
                level = 85,
                temperature = 25.0f,
                voltage = 3.7f,
                cycleCount = 100,
                estimatedRemainingLife = 365 * 24 * 60 * 60 * 1000L
            ),
            sensorHealth = emptyMap(),
            connectionHealth = ConnectionHealth(
                signalStrength = 80,
                packetLoss = 0.01f,
                latency = 50L,
                connectionStability = 0.95f
            ),
            lastMaintenance = null,
            recommendedActions = emptyList()
        )
    }
    
    private fun createMockDataSyncProgressWithConflicts(): DataSyncProgress {
        return DataSyncProgress(
            stage = SyncStage.RESOLVING_CONFLICTS,
            progress = 0.75f,
            currentStep = "Resolving conflicts",
            totalSteps = 4,
            recordsProcessed = 75,
            recordsTotal = 100,
            estimatedTimeRemaining = 5000L,
            conflicts = listOf(
                DataConflict(
                    recordId = "record_1",
                    conflictType = ConflictType.VALUE_MISMATCH,
                    serverValue = "server_value",
                    clientValue = "client_value",
                    timestamp = System.currentTimeMillis()
                )
            )
        )
    }
}
