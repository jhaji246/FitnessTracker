package com.avi.core.data.utils

import android.os.SystemClock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * Performance monitoring utilities for tracking operation timing and metrics.
 * Provides lightweight performance monitoring without external dependencies.
 */
object PerformanceUtils {
    
    // Active operations tracking
    private val activeOperations = ConcurrentHashMap<String, Long>()
    
    // Performance metrics storage
    private val operationMetrics = ConcurrentHashMap<String, MutableList<Long>>()
    
    // Performance thresholds for warnings
    private val performanceThresholds = ConcurrentHashMap<String, Long>()
    
    /**
     * Start timing an operation
     */
    fun startOperation(operationName: String) {
        val startTime = SystemClock.elapsedRealtime()
        activeOperations[operationName] = startTime
        
        Timber.d("Started performance monitoring for: $operationName")
    }
    
    /**
     * End timing an operation and log the duration
     */
    fun endOperation(operationName: String): Long {
        val startTime = activeOperations.remove(operationName)
        if (startTime == null) {
            Timber.w("Attempted to end operation that wasn't started: $operationName")
            return -1
        }
        
        val duration = SystemClock.elapsedRealtime() - startTime
        
        // Store metric
        operationMetrics.getOrPut(operationName) { mutableListOf() }.add(duration)
        
        // Check performance threshold
        checkPerformanceThreshold(operationName, duration)
        
        Timber.d("Operation completed: $operationName in ${duration}ms")
        return duration
    }
    
    /**
     * End operation with error and log failure
     */
    fun endOperationWithError(operationName: String, errorMessage: String): Long {
        val startTime = activeOperations.remove(operationName)
        if (startTime == null) {
            Timber.w("Attempted to end operation that wasn't started: $operationName")
            return -1
        }
        
        val duration = SystemClock.elapsedRealtime() - startTime
        
        // Store metric (negative to indicate error)
        operationMetrics.getOrPut(operationName) { mutableListOf() }.add(-duration)
        
        Timber.w("Operation failed: $operationName after ${duration}ms - $errorMessage")
        return duration
    }
    
    /**
     * Set performance threshold for an operation
     */
    fun setPerformanceThreshold(operationName: String, thresholdMs: Long) {
        performanceThresholds[operationName] = thresholdMs
        Timber.d("Set performance threshold for $operationName: ${thresholdMs}ms")
    }
    
    /**
     * Check if operation exceeds performance threshold
     */
    private fun checkPerformanceThreshold(operationName: String, duration: Long) {
        val threshold = performanceThresholds[operationName]
        if (threshold != null && duration > threshold) {
            Timber.w("Performance threshold exceeded: $operationName took ${duration}ms (threshold: ${threshold}ms)")
        }
    }
    
    /**
     * Get performance statistics for an operation
     */
    fun getOperationStats(operationName: String): OperationStats? {
        val metrics = operationMetrics[operationName] ?: return null
        
        if (metrics.isEmpty()) return null
        
        val successfulMetrics = metrics.filter { it > 0 }
        val failedMetrics = metrics.filter { it < 0 }
        
        return OperationStats(
            operationName = operationName,
            totalExecutions = metrics.size,
            successfulExecutions = successfulMetrics.size,
            failedExecutions = failedMetrics.size,
            averageDuration = if (successfulMetrics.isNotEmpty()) successfulMetrics.average() else 0.0,
            minDuration = if (successfulMetrics.isNotEmpty()) successfulMetrics.minOrNull() ?: 0L else 0L,
            maxDuration = if (successfulMetrics.isNotEmpty()) successfulMetrics.maxOrNull() ?: 0L else 0L,
            totalDuration = successfulMetrics.sum(),
            successRate = successfulMetrics.size.toDouble() / metrics.size
        )
    }
    
    /**
     * Get all operation statistics
     */
    fun getAllOperationStats(): List<OperationStats> {
        return operationMetrics.keys.mapNotNull { getOperationStats(it) }
    }
    
    /**
     * Clear performance data for an operation
     */
    fun clearOperationData(operationName: String) {
        activeOperations.remove(operationName)
        operationMetrics.remove(operationName)
        performanceThresholds.remove(operationName)
        Timber.d("Cleared performance data for: $operationName")
    }
    
    /**
     * Clear all performance data
     */
    fun clearAllPerformanceData() {
        activeOperations.clear()
        operationMetrics.clear()
        performanceThresholds.clear()
        Timber.d("Cleared all performance data")
    }
    
    /**
     * Check if an operation is currently running
     */
    fun isOperationRunning(operationName: String): Boolean {
        return activeOperations.containsKey(operationName)
    }
    
    /**
     * Get currently running operations
     */
    fun getRunningOperations(): Set<String> {
        return activeOperations.keys.toSet()
    }
    
    /**
     * Force stop all running operations (useful for cleanup)
     */
    fun forceStopAllOperations() {
        val runningOps = activeOperations.keys.toList()
        runningOps.forEach { operationName ->
            endOperationWithError(operationName, "Force stopped")
        }
        Timber.w("Force stopped ${runningOps.size} running operations")
    }
    
    /**
     * Get performance summary
     */
    fun getPerformanceSummary(): PerformanceSummary {
        val allStats = getAllOperationStats()
        
        return PerformanceSummary(
            totalOperations = allStats.size,
            totalExecutions = allStats.sumOf { it.totalExecutions },
            totalSuccessfulExecutions = allStats.sumOf { it.successfulExecutions },
            totalFailedExecutions = allStats.sumOf { it.failedExecutions },
            overallSuccessRate = if (allStats.isNotEmpty()) {
                allStats.sumOf { it.successfulExecutions }.toDouble() / allStats.sumOf { it.totalExecutions }
            } else 0.0,
            averageOperationDuration = if (allStats.isNotEmpty()) {
                allStats.map { it.averageDuration }.average()
            } else 0.0,
            runningOperations = getRunningOperations().size
        )
    }
}

/**
 * Performance statistics for an operation
 */
data class OperationStats(
    val operationName: String,
    val totalExecutions: Int,
    val successfulExecutions: Int,
    val failedExecutions: Int,
    val averageDuration: Double,
    val minDuration: Long,
    val maxDuration: Long,
    val totalDuration: Long,
    val successRate: Double
)

/**
 * Overall performance summary
 */
data class PerformanceSummary(
    val totalOperations: Int,
    val totalExecutions: Int,
    val totalSuccessfulExecutions: Int,
    val totalFailedExecutions: Int,
    val overallSuccessRate: Double,
    val averageOperationDuration: Double,
    val runningOperations: Int
)
