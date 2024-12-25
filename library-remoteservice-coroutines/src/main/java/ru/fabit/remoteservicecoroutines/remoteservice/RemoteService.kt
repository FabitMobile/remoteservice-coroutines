package ru.fabit.remoteservicecoroutines.remoteservice

import org.json.JSONObject
import ru.fabit.error.AuthFailureException
import ru.fabit.error.NoNetworkConnectionException
import ru.fabit.error.RemoteServiceError
import ru.fabit.error.RequestTimeoutError

interface RemoteService {
    @Throws(
        NoNetworkConnectionException::class,
        RequestTimeoutError::class,
        IllegalThreadStateException::class,
        AuthFailureException::class,
        RemoteServiceError::class,
        RuntimeException::class
    )
    suspend fun getRemoteJson(
        requestMethod: Int,
        relativePath: String,
        baseUrl: String? = null,
        params: Map<String, Any> = mapOf(),
        headers: Map<String, String> = mapOf()
    ): JSONObject

    fun getConfig(): RemoteServiceConfig

    companion object {
        const val DEFAULT_KEY = "item"
    }
}