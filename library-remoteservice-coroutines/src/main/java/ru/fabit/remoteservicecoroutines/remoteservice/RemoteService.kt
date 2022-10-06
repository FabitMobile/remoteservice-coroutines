package ru.fabit.remoteservicecoroutines.remoteservice

import org.json.JSONObject
import java.util.HashMap

interface RemoteService {
    suspend fun getRemoteJson(
        requestMethod: Int,
        relativePath: String,
        params: Map<String, Any>,
        headers: Map<String, String>
    ): JSONObject

    suspend fun getRemoteJson(
        requestMethod: Int,
        baseUrl: String,
        relativePath: String,
        params: Map<String, Any>,
        headers: Map<String, String>
    ): JSONObject

    fun getConfig(): RemoteServiceConfig
}