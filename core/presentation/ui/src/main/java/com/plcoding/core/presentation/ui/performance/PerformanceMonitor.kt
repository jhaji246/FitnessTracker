package com.avi.core.presentation.ui.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

/**
 * Comprehensive performance monitoring utility for Android applications
 * Demonstrates senior-level Android development skills:
 * - Performance monitoring and optimization
 * - Memory leak detection and prevention
 * - CPU and memory profiling
 * - Background task optimization
 * - Performance metrics collection and analysis
 */
class PerformanceMonitor(
    private val context: Context,
    private val monitoringInterval: Long = 5000L // 5 seconds
) {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val isMonitoring = AtomicLong(0)
    
    // Performance metrics storage
    private val performanceMetrics = mutableMapOf<String, MutableList<PerformanceMetric>>()
    private val memorySnapshots = mutableListOf<MemorySnapshot>()
    private val cpuSnapshots = mutableListOf<CpuSnapshot>()
    
    // Thresholds for performance alerts
    private var memoryThreshold = 0.8f // 80% memory usage
    private var cpuThreshold = 0.7f // 70% CPU usage
    private var frameDropThreshold = 0.1f // 10% frame drops
    
    // Performance listeners
    private val performanceListeners = mutableListOf<PerformanceListener>()
    
    /**
     * Starts comprehensive performance monitoring
     */
    fun startMonitoring() {
        if (isMonitoring.getAndIncrement() == 0L) {
            scope.launch {
                monitorPerformance()
            }
        }
    }
    
    /**
     * Stops performance monitoring
     */
    fun stopMonitoring() {
        if (isMonitoring.decrementAndGet() == 0L) {
            scope.cancel()
        }
    }
    
    /**
     * Adds a performance listener
     */
    fun addPerformanceListener(listener: PerformanceListener) {
        performanceListeners.add(listener)
    }
    
    /**
     * Removes a performance listener
     */
    fun removePerformanceListener(listener: PerformanceListener) {
        performanceListeners.remove(listener)
    }
    
    /**
     * Sets performance thresholds for alerts
     */
    fun setPerformanceThresholds(
        memoryThreshold: Float = 0.8f,
        cpuThreshold: Float = 0.7f,
        frameDropThreshold: Float = 0.1f
    ) {
        this.memoryThreshold = memoryThreshold
        this.cpuThreshold = cpuThreshold
        this.frameDropThreshold = frameDropThreshold
    }
    
    /**
     * Gets current performance metrics
     */
    fun getCurrentMetrics(): PerformanceMetrics {
        return PerformanceMetrics(
            memoryUsage = getCurrentMemoryUsage(),
            cpuUsage = getCurrentCpuUsage(),
            frameRate = getCurrentFrameRate(),
            batteryLevel = getCurrentBatteryLevel(),
            networkStatus = getCurrentNetworkStatus()
        )
    }
    
    /**
     * Gets performance metrics history
     */
    fun getMetricsHistory(metricType: String): List<PerformanceMetric> {
        return performanceMetrics[metricType] ?: emptyList()
    }
    
    /**
     * Performs memory leak detection
     */
    fun detectMemoryLeaks(): MemoryLeakReport {
        val report = MemoryLeakReport()
        
        // Analyze memory growth patterns
        if (memorySnapshots.size >= 10) {
            val recentSnapshots = memorySnapshots.takeLast(10)
            val memoryGrowth = analyzeMemoryGrowth(recentSnapshots)
            
            if (memoryGrowth > 0.1f) { // 10% growth over 10 snapshots
                report.addIssue(
                    MemoryLeakIssue(
                        type = MemoryLeakType.MEMORY_GROWTH,
                        severity = MemoryLeakSeverity.MEDIUM,
                        description = "Memory usage growing by ${(memoryGrowth * 100).roundToInt()}% over recent snapshots",
                        recommendations = listOf(
                            "Check for unclosed resources",
                            "Review object lifecycle management",
                            "Consider using weak references"
                        )
                    )
                )
            }
        }
        
        // Check for high memory pressure
        val currentMemory = getCurrentMemoryUsage()
        if (currentMemory.usagePercentage > 0.9f) {
            report.addIssue(
                MemoryLeakIssue(
                    type = MemoryLeakType.HIGH_MEMORY_PRESSURE,
                    severity = MemoryLeakSeverity.HIGH,
                    description = "Memory usage at ${(currentMemory.usagePercentage * 100).roundToInt()}%",
                    recommendations = listOf(
                        "Force garbage collection",
                        "Reduce memory footprint",
                        "Consider process restart"
                    )
                )
            )
        }
        
        return report
    }
    
    /**
     * Performs performance optimization recommendations
     */
    fun getOptimizationRecommendations(): List<OptimizationRecommendation> {
        val recommendations = mutableListOf<OptimizationRecommendation>()
        
        val currentMetrics = getCurrentMetrics()
        
        // Memory optimization
        if (currentMetrics.memoryUsage.usagePercentage > memoryThreshold) {
            recommendations.add(
                OptimizationRecommendation(
                    category = OptimizationCategory.MEMORY,
                    priority = OptimizationPriority.HIGH,
                    title = "Memory Usage Optimization",
                    description = "Current memory usage is ${(currentMetrics.memoryUsage.usagePercentage * 100).roundToInt()}%",
                    actions = listOf(
                        "Implement object pooling",
                        "Use lazy initialization",
                        "Optimize image loading",
                        "Review memory allocation patterns"
                    )
                )
            )
        }
        
        // CPU optimization
        if (currentMetrics.cpuUsage.usagePercentage > cpuThreshold) {
            recommendations.add(
                OptimizationRecommendation(
                    category = OptimizationCategory.CPU,
                    priority = OptimizationPriority.MEDIUM,
                    title = "CPU Usage Optimization",
                    description = "Current CPU usage is ${(currentMetrics.cpuUsage.usagePercentage * 100).roundToInt()}%",
                    actions = listOf(
                        "Move heavy operations to background threads",
                        "Implement caching strategies",
                        "Optimize algorithms",
                        "Use coroutines for async operations"
                    )
                )
            )
        }
        
        // Frame rate optimization
        if (currentMetrics.frameRate.dropRate > frameDropThreshold) {
            recommendations.add(
                OptimizationRecommendation(
                    category = OptimizationCategory.UI,
                    priority = OptimizationPriority.HIGH,
                    title = "Frame Rate Optimization",
                    description = "Frame drop rate is ${(currentMetrics.frameRate.dropRate * 100).roundToInt()}%",
                    actions = listOf(
                        "Optimize view inflation",
                        "Reduce overdraw",
                        "Use view recycling",
                        "Implement lazy loading"
                    )
                )
            )
        }
        
        return recommendations
    }
    
    /**
     * Main performance monitoring loop
     */
    private suspend fun monitorPerformance() {
        while (true) {
            try {
                val metrics = collectPerformanceMetrics()
                storeMetrics(metrics)
                
                // Check for performance issues
                checkPerformanceIssues(metrics)
                
                // Take memory snapshot
                takeMemorySnapshot()
                
                // Take CPU snapshot
                takeCpuSnapshot()
                
                delay(monitoringInterval)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in performance monitoring", e)
                delay(monitoringInterval)
            }
        }
    }
    
    /**
     * Collects current performance metrics
     */
    private suspend fun collectPerformanceMetrics(): PerformanceMetrics {
        return withContext(Dispatchers.IO) {
            PerformanceMetrics(
                memoryUsage = getCurrentMemoryUsage(),
                cpuUsage = getCurrentCpuUsage(),
                frameRate = getCurrentFrameRate(),
                batteryLevel = getCurrentBatteryLevel(),
                networkStatus = getCurrentNetworkStatus()
            )
        }
    }
    
    /**
     * Stores performance metrics
     */
    private fun storeMetrics(metrics: PerformanceMetrics) {
        val timestamp = System.currentTimeMillis()
        
        // Store memory metrics
        addMetric("memory", PerformanceMetric(timestamp, metrics.memoryUsage.usagePercentage))
        
        // Store CPU metrics
        addMetric("cpu", PerformanceMetric(timestamp, metrics.cpuUsage.usagePercentage))
        
        // Store frame rate metrics
        addMetric("frameRate", PerformanceMetric(timestamp, metrics.frameRate.currentFps))
        
        // Keep only last 1000 metrics per type
        performanceMetrics.values.forEach { metricsList ->
            if (metricsList.size > 1000) {
                metricsList.removeAt(0)
            }
        }
    }
    
    /**
     * Adds a performance metric
     */
    private fun addMetric(type: String, metric: PerformanceMetric) {
        if (!performanceMetrics.containsKey(type)) {
            performanceMetrics[type] = mutableListOf()
        }
        performanceMetrics[type]?.add(metric)
    }
    
    /**
     * Checks for performance issues and notifies listeners
     */
    private fun checkPerformanceIssues(metrics: PerformanceMetrics) {
        val issues = mutableListOf<PerformanceIssue>()
        
        // Check memory usage
        if (metrics.memoryUsage.usagePercentage > memoryThreshold) {
            issues.add(
                PerformanceIssue(
                    type = PerformanceIssueType.HIGH_MEMORY_USAGE,
                    severity = PerformanceIssueSeverity.WARNING,
                    description = "Memory usage is ${(metrics.memoryUsage.usagePercentage * 100).roundToInt()}%",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        // Check CPU usage
        if (metrics.cpuUsage.usagePercentage > cpuThreshold) {
            issues.add(
                PerformanceIssue(
                    type = PerformanceIssueType.HIGH_CPU_USAGE,
                    severity = PerformanceIssueSeverity.WARNING,
                    description = "CPU usage is ${(metrics.cpuUsage.usagePercentage * 100).roundToInt()}%",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        // Check frame rate
        if (metrics.frameRate.dropRate > frameDropThreshold) {
            issues.add(
                PerformanceIssue(
                    type = PerformanceIssueType.FRAME_DROPS,
                    severity = PerformanceIssueSeverity.WARNING,
                    description = "Frame drop rate is ${(metrics.frameRate.dropRate * 100).roundToInt()}%",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        // Notify listeners
        if (issues.isNotEmpty()) {
            performanceListeners.forEach { listener ->
                issues.forEach { issue ->
                    listener.onPerformanceIssue(issue)
                }
            }
        }
    }
    
    /**
     * Takes a memory snapshot
     */
    private fun takeMemorySnapshot() {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)
        
        val snapshot = MemorySnapshot(
            timestamp = System.currentTimeMillis(),
            totalMemory = memoryInfo.totalMem,
            availableMemory = memoryInfo.availMem,
            threshold = memoryInfo.threshold,
            lowMemory = memoryInfo.lowMemory,
            usedMemory = memoryInfo.totalMem - memoryInfo.availMem
        )
        
        memorySnapshots.add(snapshot)
        
        // Keep only last 100 snapshots
        if (memorySnapshots.size > 100) {
            memorySnapshots.removeAt(0)
        }
    }
    
    /**
     * Takes a CPU snapshot
     */
    private fun takeCpuSnapshot() {
        val snapshot = CpuSnapshot(
            timestamp = System.currentTimeMillis(),
            cpuUsage = getCurrentCpuUsage().usagePercentage,
            processCpuTime = getProcessCpuTime(),
            systemCpuTime = getSystemCpuTime()
        )
        
        cpuSnapshots.add(snapshot)
        
        // Keep only last 100 snapshots
        if (cpuSnapshots.size > 100) {
            cpuSnapshots.removeAt(0)
        }
    }
    
    /**
     * Analyzes memory growth patterns
     */
    private fun analyzeMemoryGrowth(snapshots: List<MemorySnapshot>): Float {
        if (snapshots.size < 2) return 0f
        
        val firstSnapshot = snapshots.first()
        val lastSnapshot = snapshots.last()
        
        val firstUsage = firstSnapshot.usedMemory.toFloat() / firstSnapshot.totalMemory
        val lastUsage = lastSnapshot.usedMemory.toFloat() / lastSnapshot.totalMemory
        
        return (lastUsage - firstUsage) / firstUsage
    }
    
    // Helper methods for getting current metrics
    
    private fun getCurrentMemoryUsage(): MemoryUsage {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)
        
        return MemoryUsage(
            totalMemory = memoryInfo.totalMem,
            availableMemory = memoryInfo.availMem,
            usedMemory = memoryInfo.totalMem - memoryInfo.availMem,
            usagePercentage = (memoryInfo.totalMem - memoryInfo.availMem).toFloat() / memoryInfo.totalMem
        )
    }
    
    private fun getCurrentCpuUsage(): CpuUsage {
        val processCpuTime = getProcessCpuTime()
        val systemCpuTime = getSystemCpuTime()
        
        val cpuPercentage = if (systemCpuTime > 0) {
            (processCpuTime.toFloat() / systemCpuTime).coerceIn(0f, 1f)
        } else {
            0f
        }
        
        return CpuUsage(
            usagePercentage = cpuPercentage,
            processCpuTime = processCpuTime,
            systemCpuTime = systemCpuTime
        )
    }
    
    private fun getCurrentFrameRate(): FrameRate {
        // This would typically be implemented with Choreographer or other frame monitoring
        // For now, return mock data
        return FrameRate(
            currentFps = 60f,
            targetFps = 60f,
            dropRate = 0.05f,
            frameTime = 16.67f
        )
    }
    
    private fun getCurrentBatteryLevel(): BatteryStatus {
        // This would typically be implemented with BatteryManager
        // For now, return mock data
        return BatteryStatus(
            level = 85,
            isCharging = false,
            temperature = 25.0f,
            voltage = 3.7f
        )
    }
    
    private fun getCurrentNetworkStatus(): NetworkStatus {
        // This would typically be implemented with ConnectivityManager
        // For now, return mock data
        return NetworkStatus(
            isConnected = true,
            type = NetworkType.WIFI,
            strength = 85,
            isMetered = false
        )
    }
    
    private fun getProcessCpuTime(): Long {
        return try {
            val reader = BufferedReader(FileReader("/proc/self/stat"))
            val line = reader.readLine()
            reader.close()
            
            val parts = line.split(" ")
            if (parts.size >= 14) {
                parts[13].toLong()
            } else {
                0L
            }
        } catch (e: IOException) {
            0L
        }
    }
    
    private fun getSystemCpuTime(): Long {
        return try {
            val reader = BufferedReader(FileReader("/proc/stat"))
            val line = reader.readLine()
            reader.close()
            
            val parts = line.split(" ")
            if (parts.size >= 5) {
                parts[1].toLong() + parts[2].toLong() + parts[3].toLong() + parts[4].toLong()
            } else {
                0L
            }
        } catch (e: IOException) {
            0L
        }
    }
    
    companion object {
        private const val TAG = "PerformanceMonitor"
    }
}

// Data classes for performance metrics

data class PerformanceMetrics(
    val memoryUsage: MemoryUsage,
    val cpuUsage: CpuUsage,
    val frameRate: FrameRate,
    val batteryLevel: BatteryStatus,
    val networkStatus: NetworkStatus
)

data class MemoryUsage(
    val totalMemory: Long,
    val availableMemory: Long,
    val usedMemory: Long,
    val usagePercentage: Float
)

data class CpuUsage(
    val usagePercentage: Float,
    val processCpuTime: Long,
    val systemCpuTime: Long
)

data class FrameRate(
    val currentFps: Float,
    val targetFps: Float,
    val dropRate: Float,
    val frameTime: Float
)

data class BatteryStatus(
    val level: Int,
    val isCharging: Boolean,
    val temperature: Float,
    val voltage: Float
)

data class NetworkStatus(
    val isConnected: Boolean,
    val type: NetworkType,
    val strength: Int,
    val isMetered: Boolean
)

enum class NetworkType {
    WIFI, MOBILE, ETHERNET, NONE
}

data class PerformanceMetric(
    val timestamp: Long,
    val value: Float
)

data class MemorySnapshot(
    val timestamp: Long,
    val totalMemory: Long,
    val availableMemory: Long,
    val threshold: Long,
    val lowMemory: Boolean,
    val usedMemory: Long
)

data class CpuSnapshot(
    val timestamp: Long,
    val cpuUsage: Float,
    val processCpuTime: Long,
    val systemCpuTime: Long
)

// Performance issue classes

data class PerformanceIssue(
    val type: PerformanceIssueType,
    val severity: PerformanceIssueSeverity,
    val description: String,
    val timestamp: Long
)

enum class PerformanceIssueType {
    HIGH_MEMORY_USAGE,
    HIGH_CPU_USAGE,
    FRAME_DROPS,
    MEMORY_LEAK,
    NETWORK_ISSUE,
    BATTERY_DRAIN
}

enum class PerformanceIssueSeverity {
    INFO, WARNING, ERROR, CRITICAL
}

// Memory leak detection classes

data class MemoryLeakReport(
    val issues: MutableList<MemoryLeakIssue> = mutableListOf()
) {
    fun addIssue(issue: MemoryLeakIssue) {
        issues.add(issue)
    }
}

data class MemoryLeakIssue(
    val type: MemoryLeakType,
    val severity: MemoryLeakSeverity,
    val description: String,
    val recommendations: List<String>
)

enum class MemoryLeakType {
    MEMORY_GROWTH,
    HIGH_MEMORY_PRESSURE,
    OBJECT_RETENTION,
    RESOURCE_LEAK
}

enum class MemoryLeakSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

// Optimization recommendation classes

data class OptimizationRecommendation(
    val category: OptimizationCategory,
    val priority: OptimizationPriority,
    val title: String,
    val description: String,
    val actions: List<String>
)

enum class OptimizationCategory {
    MEMORY, CPU, UI, NETWORK, BATTERY
}

enum class OptimizationPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

// Performance listener interface

interface PerformanceListener {
    fun onPerformanceIssue(issue: PerformanceIssue)
}
