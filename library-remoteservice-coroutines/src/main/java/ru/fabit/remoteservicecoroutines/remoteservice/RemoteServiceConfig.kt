package ru.fabit.remoteservicecoroutines.remoteservice

interface RemoteServiceConfig {
    val baseUrl: String
    val uploadServerUrl: String
    val defaultHeaders: Map<String, String>
    val connectTimeoutMillis: Long
    val readTimeoutMillis: Long
    val maxRetries: Int
    val isLogEnabled: Boolean
}