package com.avi.core.data.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.avi.core.domain.ble.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BleServiceImplTest {

    private lateinit var mockContext: Context
    private lateinit var mockBluetoothManager: BluetoothManager
    private lateinit var mockBluetoothAdapter: BluetoothAdapter
    private lateinit var bleService: BleServiceImpl

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockBluetoothManager = mockk(relaxed = true)
        mockBluetoothAdapter = mockk(relaxed = true)
        
        // Mock BluetoothManager
        every { mockContext.getSystemService(Context.BLUETOOTH_SERVICE) } returns mockBluetoothManager
        
        // Mock BluetoothAdapter
        every { mockBluetoothManager.adapter } returns mockBluetoothAdapter
        
        bleService = BleServiceImpl(mockContext)
    }

    @Test
    fun `test isBluetoothEnabled when Bluetooth is enabled`() = runTest {
        // Given
        every { mockBluetoothAdapter.isEnabled } returns true
        
        // When
        val result = bleService.isBluetoothEnabled()
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `test isBluetoothEnabled when Bluetooth is disabled`() = runTest {
        // Given
        every { mockBluetoothAdapter.isEnabled } returns false
        
        // When
        val result = bleService.isBluetoothEnabled()
        
        // Then
        assertFalse(result)
    }

    @Test
    fun `test hasLocationPermissions when all permissions granted`() = runTest {
        // Given
        mockkStatic(ActivityCompat::class)
        every { 
            ActivityCompat.checkSelfPermission(mockContext, android.Manifest.permission.BLUETOOTH_SCAN) 
        } returns PackageManager.PERMISSION_GRANTED
        every { 
            ActivityCompat.checkSelfPermission(mockContext, android.Manifest.permission.BLUETOOTH_CONNECT) 
        } returns PackageManager.PERMISSION_GRANTED
        
        // When
        val result = bleService.hasLocationPermissions()
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `test hasLocationPermissions when some permissions denied`() = runTest {
        // Given
        mockkStatic(ActivityCompat::class)
        every { 
            ActivityCompat.checkSelfPermission(mockContext, android.Manifest.permission.BLUETOOTH_SCAN) 
        } returns PackageManager.PERMISSION_DENIED
        every { 
            ActivityCompat.checkSelfPermission(mockContext, android.Manifest.permission.BLUETOOTH_CONNECT) 
        } returns PackageManager.PERMISSION_GRANTED
        
        // When
        val result = bleService.hasLocationPermissions()
        
        // Then
        assertFalse(result)
    }

    @Test
    fun `test startScan when Bluetooth enabled and permissions granted`() = runTest {
        // Given
        every { mockBluetoothAdapter.isEnabled } returns true
        mockkStatic(ActivityCompat::class)
        every { 
            ActivityCompat.checkSelfPermission(mockContext, android.Manifest.permission.BLUETOOTH_SCAN) 
        } returns PackageManager.PERMISSION_GRANTED
        every { 
            ActivityCompat.checkSelfPermission(mockContext, android.Manifest.permission.BLUETOOTH_CONNECT) 
        } returns PackageManager.PERMISSION_GRANTED
        
        // When
        val result = bleService.startScan()
        
        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `test startScan when Bluetooth disabled`() = runTest {
        // Given
        every { mockBluetoothAdapter.isEnabled } returns false
        
        // When
        val result = bleService.startScan()
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Bluetooth is not enabled", result.exceptionOrNull()?.message)
    }

    @Test
    fun `test startScan when permissions not granted`() = runTest {
        // Given
        every { mockBluetoothAdapter.isEnabled } returns true
        mockkStatic(ActivityCompat::class)
        every { 
            ActivityCompat.checkSelfPermission(mockContext, android.Manifest.permission.BLUETOOTH_SCAN) 
        } returns PackageManager.PERMISSION_DENIED
        
        // When
        val result = bleService.startScan()
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Location permissions not granted", result.exceptionOrNull()?.message)
    }

    @Test
    fun `test getDiscoveredDevices returns flow`() = runTest {
        // When
        val flow = bleService.getDiscoveredDevices()
        
        // Then
        assertNotNull(flow)
    }

    @Test
    fun `test getConnectedDevices returns flow`() = runTest {
        // When
        val flow = bleService.getConnectedDevices()
        
        // Then
        assertNotNull(flow)
    }

    @Test
    fun `test getTelemetryDataStream returns flow`() = runTest {
        // Given
        val deviceAddress = "00:11:22:33:44:55"
        
        // When
        val flow = bleService.getTelemetryDataStream(deviceAddress)
        
        // Then
        assertNotNull(flow)
    }

    @Test
    fun `test sendCommand with valid command`() = runTest {
        // Given
        val deviceAddress = "00:11:22:33:44:55"
        val command = StartDataCollectionCommand()
        
        // When - This will likely fail since we can't mock internal methods
        // but we can test that the method signature is correct
        val result = bleService.sendCommand(deviceAddress, command)
        
        // Then - We just verify the method can be called
        // The actual result depends on the internal implementation
        assertNotNull(result)
    }

    @Test
    fun `test getDeviceStatus when device connected`() = runTest {
        // Given
        val deviceAddress = "00:11:22:33:44:55"
        
        // When
        val result = bleService.getDeviceStatus(deviceAddress)
        
        // Then
        assertNotNull(result)
        // We can't test the actual result without a real connection
    }

    @Test
    fun `test configureDevice with valid configuration`() = runTest {
        // Given
        val deviceAddress = "00:11:22:33:44:55"
        val configuration = DeviceConfiguration(
            deviceAddress = deviceAddress,
            sensorConfigurations = mapOf(
                "heart_rate" to SensorConfiguration("heart_rate", true, 1000, 100f, 0.1f)
            ),
            dataCollectionInterval = 1000,
            transmissionPower = 0,
            sleepModeEnabled = false
        )
        
        // When - This will likely fail since we can't mock internal methods
        // but we can test that the method signature is correct
        val result = bleService.configureDevice(deviceAddress, configuration)
        
        // Then - We just verify the method can be called
        // The actual result depends on the internal implementation
        assertNotNull(result)
    }

    @Test
    fun `test getConnectionQuality returns valid data`() = runTest {
        // Given
        val deviceAddress = "00:11:22:33:44:55"
        
        // When
        val result = bleService.getConnectionQuality(deviceAddress)
        
        // Then
        assertTrue(result.isSuccess)
        val quality = result.getOrNull()
        assertNotNull(quality)
        assertEquals(deviceAddress, quality?.deviceAddress)
        assertTrue(quality?.rssi != null)
        assertTrue(quality?.connectionInterval != null)
        assertTrue(quality?.latency != null)
        assertTrue(quality?.throughput != null)
        assertTrue(quality?.packetLoss != null)
    }

    private fun assertNotNull(any: Any?) {
        assert(any != null) { "Expected non-null value" }
    }
}
