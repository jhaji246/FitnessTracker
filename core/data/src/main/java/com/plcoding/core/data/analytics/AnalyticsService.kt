package com.avi.core.data.analytics

import android.content.Context
import android.os.Bundle
import com.avi.core.data.firebase.FirebaseService
import com.avi.core.data.jni.NativePerformanceBridge
import com.avi.core.data.utils.PerformanceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber


/**
 * Comprehensive analytics service integrating Firebase, Google Play Console, and custom analytics.
 * Demonstrates senior-level analytics implementation and cross-platform tracking.
 * 
 * Note: This is a simplified implementation that demonstrates the architecture.
 * In production, this would integrate with actual Firebase and analytics services.
 */

class AnalyticsService(
    private val context: Context,
    private val firebaseService: FirebaseService,
    private val nativeBridge: NativePerformanceBridge
) {
    
    // Analytics configuration
    private var isAnalyticsEnabled = true
    private var isCrashlyticsEnabled = true
    private var isPerformanceMonitoringEnabled = true
    private var isUserTrackingEnabled = true
    
    // User session tracking
    private var sessionStartTime: Long = 0
    private var sessionId: String = ""
    private var currentUserId: String? = null
    
    // Performance tracking
    private val operationTimings = mutableMapOf<String, MutableList<Long>>()
    
    init {
        initializeAnalytics()
    }
    
    /**
     * Initialize analytics services
     */
    private fun initializeAnalytics() {
        try {
            // Set default user properties
            setUserProperty("app_version", getAppVersion())
            setUserProperty("device_model", getDeviceModel())
            setUserProperty("android_version", getAndroidVersion())
            setUserProperty("app_install_source", getInstallSource())
            
            // Start session tracking
            startSession()
            
            Timber.d("Analytics service initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize analytics service")
        }
    }
    
    /**
     * Start a new user session
     */
    fun startSession() {
        sessionStartTime = System.currentTimeMillis()
        sessionId = generateSessionId()
        
        val sessionData = Bundle().apply {
            putString("session_id", sessionId)
            putLong("start_time", sessionStartTime)
            putString("device_id", getDeviceId())
        }
        
        logEvent("session_started", sessionData)
        Timber.d("Started analytics session: $sessionId")
    }
    
    /**
     * End current user session
     */
    fun endSession() {
        val sessionDuration = System.currentTimeMillis() - sessionStartTime
        
        val sessionData = Bundle().apply {
            putString("session_id", sessionId)
            putLong("duration_ms", sessionDuration)
            putString("end_reason", "user_exit")
        }
        
        logEvent("session_ended", sessionData)
        Timber.d("Ended analytics session: $sessionId (duration: ${sessionDuration}ms)")
        
        // Reset session data
        sessionId = ""
        sessionStartTime = 0
    }
    
    /**
     * Set user ID for cross-platform tracking
     */
    fun setUserId(userId: String?) {
        currentUserId = userId
        
        try {
            firebaseService.setUserId(userId)
            
            val userData = Bundle().apply {
                putString("user_id", userId)
                putString("session_id", sessionId)
                putLong("timestamp", System.currentTimeMillis())
            }
            
            logEvent("user_identified", userData)
            Timber.d("Set user ID: $userId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to set user ID")
        }
    }
    
    /**
     * Log custom events with parameters
     */
    fun logEvent(eventName: String, parameters: Bundle? = null) {
        if (!isAnalyticsEnabled) return
        
        try {
            // Add common parameters
            val enhancedParams = parameters ?: Bundle()
            enhancedParams.putString("session_id", sessionId)
            enhancedParams.putString("user_id", currentUserId)
            enhancedParams.putLong("timestamp", System.currentTimeMillis())
            enhancedParams.putString("device_id", getDeviceId())
            
            // Log to Firebase service
            firebaseService.logEvent(eventName, enhancedParams)
            
            // Log to custom analytics
            logCustomEvent(eventName, enhancedParams)
            
            Timber.d("Logged analytics event: $eventName")
        } catch (e: Exception) {
            Timber.e(e, "Failed to log analytics event: $eventName")
        }
    }
    
    /**
     * Log user properties for segmentation
     */
    fun setUserProperty(name: String, value: String?) {
        if (!isUserTrackingEnabled) return
        
        try {
            firebaseService.setUserProperty(name, value)
            
            val propertyData = Bundle().apply {
                putString("property_name", name)
                putString("property_value", value)
                putString("user_id", currentUserId)
            }
            
            logEvent("user_property_set", propertyData)
            Timber.d("Set user property: $name = $value")
        } catch (e: Exception) {
            Timber.e(e, "Failed to set user property: $name")
        }
    }
    
    /**
     * Start performance monitoring for an operation
     */
    fun startPerformanceTrace(traceName: String): String {
        if (!isPerformanceMonitoringEnabled) return ""
        
        val traceId = "${traceName}_${System.currentTimeMillis()}"
        
        try {
            // Start native performance monitoring
            PerformanceUtils.startOperation(traceName)
            
            Timber.d("Started performance trace: $traceId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start performance trace: $traceName")
        }
        
        return traceId
    }
    
    /**
     * Stop performance monitoring and log results
     */
    fun stopPerformanceTrace(traceId: String, success: Boolean = true) {
        if (!isPerformanceMonitoringEnabled) return
        
        try {
            // Stop native performance monitoring
            val operationName = traceId.substringBeforeLast("_")
            if (success) {
                PerformanceUtils.endOperation(operationName)
            } else {
                PerformanceUtils.endOperationWithError(operationName, "Operation failed")
            }
            
            // Log performance metrics
            logPerformanceMetrics(operationName, success)
            
            Timber.d("Stopped performance trace: $traceId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop performance trace: $traceId")
        }
    }
    
    /**
     * Log performance metrics
     */
    private fun logPerformanceMetrics(operationName: String, success: Boolean) {
        val metrics = Bundle().apply {
            putString("operation_name", operationName)
            putBoolean("success", success)
            putString("session_id", sessionId)
            putString("user_id", currentUserId)
        }
        
        logEvent("performance_metrics", metrics)
    }
    
    /**
     * Log fitness activity events
     */
    fun logFitnessActivity(
        activityType: String,
        duration: Long,
        distance: Float,
        calories: Int,
        heartRate: Int? = null,
        elevation: Float? = null
    ) {
        val activityData = Bundle().apply {
            putString("activity_type", activityType)
            putLong("duration_seconds", duration)
            putFloat("distance_km", distance)
            putInt("calories_burned", calories)
            putString("session_id", sessionId)
            putString("user_id", currentUserId)
            
            heartRate?.let { putInt("heart_rate", it) }
            elevation?.let { putFloat("elevation_m", it) }
        }
        
        logEvent("fitness_activity", activityData)
        
        // Also log to Firebase service for additional tracking
        firebaseService.logFitnessEvent(activityType, duration, distance, calories)
    }
    
    /**
     * Log device connectivity events
     */
    fun logDeviceEvent(deviceType: String, event: String, success: Boolean, metadata: Map<String, String> = emptyMap()) {
        val deviceData = Bundle().apply {
            putString("device_type", deviceType)
            putString("event", event)
            putBoolean("success", success)
            putString("session_id", sessionId)
            putString("user_id", currentUserId)
            
            metadata.forEach { (key, value) ->
                putString(key, value)
            }
        }
        
        logEvent("device_event", deviceData)
        
        // Also log to Firebase service
        firebaseService.logDeviceEvent(deviceType, event, success)
    }
    
    /**
     * Log app lifecycle events
     */
    fun logAppLifecycleEvent(event: AppLifecycleEvent, metadata: Map<String, String> = emptyMap()) {
        val lifecycleData = Bundle().apply {
            putString("event_type", event.name)
            putString("session_id", sessionId)
            putString("user_id", currentUserId)
            putLong("timestamp", System.currentTimeMillis())
            
            metadata.forEach { (key, value) ->
                putString(key, value)
            }
        }
        
        logEvent("app_lifecycle", lifecycleData)
        
        // Also log to Firebase service
        firebaseService.logAppLifecycleEvent(event.name)
    }
    
    /**
     * Log error events with context
     */
    fun logError(error: Throwable, context: String, metadata: Map<String, String> = emptyMap()) {
        try {
            // Log to Firebase service
            firebaseService.logError(error, context)
            
            // Log custom error event
            val errorData = Bundle().apply {
                putString("error_type", error.javaClass.simpleName)
                putString("error_message", error.message ?: "Unknown error")
                putString("error_context", context)
                putString("session_id", sessionId)
                putString("user_id", currentUserId)
                putString("stack_trace", error.stackTraceToString())
            }
            
            logEvent("error_occurred", errorData)
            
            Timber.e(error, "Logged error to analytics: $context")
        } catch (e: Exception) {
            Timber.e(e, "Failed to log error to analytics")
        }
    }
    
    /**
     * Log custom events for business intelligence
     */
    private fun logCustomEvent(eventName: String, parameters: Bundle) {
        // Here you would integrate with other analytics platforms
        // like Google Analytics, Mixpanel, Amplitude, etc.
        
        // For now, we'll just log to Timber
        Timber.d("Custom analytics event: $eventName with params: $parameters")
    }
    
    /**
     * Enable/disable analytics features
     */
    fun setAnalyticsEnabled(enabled: Boolean) {
        isAnalyticsEnabled = enabled
        Timber.d("Analytics ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun setCrashlyticsEnabled(enabled: Boolean) {
        isCrashlyticsEnabled = enabled
        firebaseService.setCrashlyticsCollectionEnabled(enabled)
        Timber.d("Crashlytics ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun setPerformanceMonitoringEnabled(enabled: Boolean) {
        isPerformanceMonitoringEnabled = enabled
        firebaseService.setPerformanceCollectionEnabled(enabled)
        Timber.d("Performance monitoring ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun setUserTrackingEnabled(enabled: Boolean) {
        isUserTrackingEnabled = enabled
        Timber.d("User tracking ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Get analytics configuration status
     */
    fun getAnalyticsStatus(): AnalyticsStatus {
        return AnalyticsStatus(
            isAnalyticsEnabled = isAnalyticsEnabled,
            isCrashlyticsEnabled = isCrashlyticsEnabled,
            isPerformanceMonitoringEnabled = isPerformanceMonitoringEnabled,
            isUserTrackingEnabled = isUserTrackingEnabled,
            sessionId = sessionId,
            currentUserId = currentUserId,
            sessionDuration = if (sessionStartTime > 0) System.currentTimeMillis() - sessionStartTime else 0
        )
    }
    
    /**
     * Utility methods for device and app information
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private fun getDeviceModel(): String {
        return android.os.Build.MODEL
    }
    
    private fun getAndroidVersion(): String {
        return android.os.Build.VERSION.RELEASE
    }
    
    private fun getInstallSource(): String {
        return try {
            val installerPackage = context.packageManager.getInstallerPackageName(context.packageName)
            if (installerPackage != null) installerPackage else "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
    }
    
    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}

/**
 * App lifecycle events for analytics
 */
enum class AppLifecycleEvent {
    APP_LAUNCH,
    APP_FOREGROUND,
    APP_BACKGROUND,
    APP_TERMINATE,
    FEATURE_ACCESS,
    ERROR_OCCURRED,
    USER_LOGIN,
    USER_LOGOUT,
    PERMISSION_GRANTED,
    PERMISSION_DENIED
}

/**
 * Analytics service status
 */
data class AnalyticsStatus(
    val isAnalyticsEnabled: Boolean,
    val isCrashlyticsEnabled: Boolean,
    val isPerformanceMonitoringEnabled: Boolean,
    val isUserTrackingEnabled: Boolean,
    val sessionId: String,
    val currentUserId: String?,
    val sessionDuration: Long
)
