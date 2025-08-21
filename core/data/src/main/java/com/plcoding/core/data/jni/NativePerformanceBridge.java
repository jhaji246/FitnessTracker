package com.avi.core.data.jni;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * JNI bridge for native performance optimization.
 * Demonstrates NDK and low-level programming skills.
 */
public class NativePerformanceBridge {
    
    static {
        try {
            System.loadLibrary("native-performance");
        } catch (UnsatisfiedLinkError e) {
            // Fallback to Java implementation if native library is not available
            System.err.println("Warning: Native performance library not available, using Java fallback");
        }
    }
    
    /**
     * Native method declarations
     */
    
    /**
     * Optimized string processing using native code
     */
    public native String processStringNative(@NonNull String input);
    
    /**
     * Fast array sorting using native implementation
     */
    public native void sortArrayNative(@NonNull int[] array);
    
    /**
     * Memory-efficient data compression
     */
    public native byte[] compressDataNative(@NonNull byte[] data);
    
    /**
     * Fast data decompression
     */
    public native byte[] decompressDataNative(@NonNull byte[] compressedData);
    
    /**
     * Cryptographic hash calculation using native implementation
     */
    public native String calculateHashNative(@NonNull byte[] data, @NonNull String algorithm);
    
    /**
     * Fast matrix multiplication for mathematical operations
     */
    public native double[] multiplyMatricesNative(@NonNull double[] matrixA, @NonNull double[] matrixB, int rowsA, int colsA, int colsB);
    
    /**
     * Real-time signal processing for sensor data
     */
    public native float[] processSensorDataNative(@NonNull float[] sensorData, int sampleRate);
    
    /**
     * Memory pool management for efficient allocation
     */
    public native long allocateMemoryPool(int size);
    public native void freeMemoryPool(long poolHandle);
    public native byte[] allocateFromPool(long poolHandle, int size);
    
    /**
     * Thread pool management for parallel processing
     */
    public native long createThreadPool(int threadCount);
    public native void destroyThreadPool(long poolHandle);
    public native void submitTask(long poolHandle, @NonNull Runnable task);
    
    /**
     * Performance monitoring at native level
     */
    public native long getNativeTimestamp();
    public native double getCpuUsage();
    public native long getMemoryUsage();
    
    /**
     * Java fallback implementations when native code is not available
     */
    
    public String processStringJava(@NonNull String input) {
        // Java fallback implementation
        return input.toUpperCase().trim();
    }
    
    public void sortArrayJava(@NonNull int[] array) {
        // Java fallback implementation
        java.util.Arrays.sort(array);
    }
    
    public byte[] compressDataJava(@NonNull byte[] data) {
        // Java fallback implementation using GZIP
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.util.zip.GZIPOutputStream gzipOut = new java.util.zip.GZIPOutputStream(baos);
            gzipOut.write(data);
            gzipOut.close();
            return baos.toByteArray();
        } catch (Exception e) {
            return data; // Return original data if compression fails
        }
    }
    
    public byte[] decompressDataJava(@NonNull byte[] compressedData) {
        // Java fallback implementation using GZIP
        try {
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(compressedData);
            java.util.zip.GZIPInputStream gzipIn = new java.util.zip.GZIPInputStream(bais);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIn.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            
            gzipIn.close();
            return baos.toByteArray();
        } catch (Exception e) {
            return compressedData; // Return compressed data if decompression fails
        }
    }
    
    public String calculateHashJava(@NonNull byte[] data, @NonNull String algorithm) {
        // Java fallback implementation
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance(algorithm);
            byte[] hashBytes = digest.digest(data);
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "hash_error";
        }
    }
    
    /**
     * Hybrid approach: try native first, fallback to Java
     */
    
    public String processString(@NonNull String input) {
        try {
            return processStringNative(input);
        } catch (UnsatisfiedLinkError e) {
            return processStringJava(input);
        }
    }
    
    public void sortArray(@NonNull int[] array) {
        try {
            sortArrayNative(array);
        } catch (UnsatisfiedLinkError e) {
            sortArrayJava(array);
        }
    }
    
    public byte[] compressData(@NonNull byte[] data) {
        try {
            return compressDataNative(data);
        } catch (UnsatisfiedLinkError e) {
            return compressDataJava(data);
        }
    }
    
    public byte[] decompressData(@NonNull byte[] compressedData) {
        try {
            return decompressDataNative(compressedData);
        } catch (UnsatisfiedLinkError e) {
            return decompressDataJava(compressedData);
        }
    }
    
    public String calculateHash(@NonNull byte[] data, @NonNull String algorithm) {
        try {
            return calculateHashNative(data, algorithm);
        } catch (UnsatisfiedLinkError e) {
            return calculateHashJava(data, algorithm);
        }
    }
    
    /**
     * Performance benchmarking utilities
     */
    public static class Benchmark {
        
        /**
         * Benchmark a native operation vs Java fallback
         */
        public static BenchmarkResult benchmarkOperation(@NonNull String operationName, @NonNull Runnable nativeOperation, @NonNull Runnable javaOperation) {
            // Warm up JVM
            for (int i = 0; i < 1000; i++) {
                javaOperation.run();
            }
            
            // Benchmark Java implementation
            long javaStart = System.nanoTime();
            for (int i = 0; i < 10000; i++) {
                javaOperation.run();
            }
            long javaEnd = System.nanoTime();
            long javaDuration = javaEnd - javaStart;
            
            // Benchmark native implementation (if available)
            long nativeDuration = -1;
            try {
                long nativeStart = System.nanoTime();
                for (int i = 0; i < 10000; i++) {
                    nativeOperation.run();
                }
                long nativeEnd = System.nanoTime();
                nativeDuration = nativeEnd - nativeStart;
            } catch (UnsatisfiedLinkError e) {
                // Native not available
            }
            
            return new BenchmarkResult(operationName, javaDuration, nativeDuration);
        }
    }
    
    /**
     * Benchmark result data class
     */
    public static class BenchmarkResult {
        private final String operationName;
        private final long javaDuration;
        private final long nativeDuration;
        
        public BenchmarkResult(String operationName, long javaDuration, long nativeDuration) {
            this.operationName = operationName;
            this.javaDuration = javaDuration;
            this.nativeDuration = nativeDuration;
        }
        
        public String getOperationName() {
            return operationName;
        }
        
        public long getJavaDuration() {
            return javaDuration;
        }
        
        public long getNativeDuration() {
            return nativeDuration;
        }
        
        public boolean isNativeAvailable() {
            return nativeDuration >= 0;
        }
        
        public double getSpeedupRatio() {
            if (!isNativeAvailable()) return 1.0;
            return (double) javaDuration / nativeDuration;
        }
        
        @Override
        @NonNull
        public String toString() {
            if (isNativeAvailable()) {
                return String.format("BenchmarkResult{operation='%s', java=%dns, native=%dns, speedup=%.2fx}",
                        operationName, javaDuration, nativeDuration, getSpeedupRatio());
            } else {
                return String.format("BenchmarkResult{operation='%s', java=%dns, native=unavailable}",
                        operationName, javaDuration);
            }
        }
    }
}
