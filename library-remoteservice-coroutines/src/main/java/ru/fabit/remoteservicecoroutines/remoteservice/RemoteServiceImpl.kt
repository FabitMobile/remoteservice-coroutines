package ru.fabit.remoteservicecoroutines.remoteservice

import android.os.Looper
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import ru.fabit.error.AuthFailureException
import ru.fabit.error.NoNetworkConnectionException
import ru.fabit.error.RemoteServiceError
import ru.fabit.error.RequestTimeoutError
import ru.fabit.remoteservicecoroutines.RetrofitApi
import ru.fabit.remoteservicecoroutines.entities.RequestMethods
import java.io.IOException
import java.net.SocketTimeoutException

class RemoteServiceImpl(
    private val retrofitBuilder: Retrofit.Builder,
    private val remoteServiceConfig: RemoteServiceConfig,
    private val remoteServiceErrorHandler: RemoteServiceErrorHandler
) : RemoteService {

    private val retrofit by lazy {
        retrofitBuilder
            .baseUrl(remoteServiceConfig.baseUrl)
            .build()
    }
    private val api by lazy { retrofit.create(RetrofitApi::class.java) }

    override suspend fun getRemoteJson(
        requestMethod: Int,
        relativePath: String,
        params: Map<String, Any>,
        headers: Map<String, String>
    ): JSONObject {
        return getRemoteJsonInner(
            requestMethod,
            remoteServiceConfig.baseUrl,
            relativePath,
            params,
            headers
        )
    }

    override suspend fun getRemoteJson(
        requestMethod: Int,
        baseUrl: String,
        relativePath: String,
        params: Map<String, Any>,
        headers: Map<String, String>
    ): JSONObject {
        return getRemoteJsonInner(requestMethod, baseUrl, relativePath, params, headers)
    }

    override fun getConfig(): RemoteServiceConfig {
        return remoteServiceConfig
    }

    private suspend fun getRemoteJsonInner(
        requestMethod: Int,
        baseUrl: String,
        relativePath: String,
        params: Map<String, Any>,
        headers: Map<String, String>
    ): JSONObject {
        val jsonObject: JSONObject
        if (Looper.getMainLooper() == Looper.myLooper()) throw IllegalThreadStateException()
        val url = baseUrl.plus(relativePath)
        try {
            val response = when (requestMethod) {
                RequestMethods.GET -> api.getObject(url, headers, params)
                RequestMethods.PUT -> api.putObject(
                    url,
                    headers,
                    getRequestBody(params)
                )
                RequestMethods.POST -> api.postObject(
                    url,
                    headers,
                    getRequestBody(params)
                )
                RequestMethods.DELETE -> api.deleteObject(
                    url,
                    headers,
                    params
                )
                RequestMethods.PATCH -> api.patchObject(
                    url,
                    headers,
                    getRequestBody(params)
                )
                else -> throw Throwable("No requestMethod")
            }
            jsonObject = mapResponseToJSONObject(response, relativePath)
        } catch (t: Throwable) {
            val throwable = onError(t)
            throw throwable
        }
        return jsonObject
    }

    private fun getRequestBody(params: Map<String, Any>?): RequestBody {
        val wrappedParams = if (params != null) {
            wrap(params)
        } else {
            mapOf<Any, Any>()
        }
        val jsonObject = JSONObject(wrappedParams)
        return jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
    }

    private fun onError(t: Throwable): Throwable {
        return when (t) {
            is SocketTimeoutException -> RequestTimeoutError(t.message)
            is IllegalThreadStateException -> IllegalThreadStateException(t.message)
            is IOException -> NoNetworkConnectionException(t.message)
            is AuthFailureException -> t
            is RemoteServiceError -> t
            is CancellationException -> t
            else -> RuntimeException(t.message)
        }
    }

    private fun mapResponseToJSONObject(
        response: Response<ResponseBody>?,
        relativePath: String?
    ): JSONObject {
        val body = response?.body()
        return response?.code()?.let { code ->
            when (code) {
                in 200..299 -> {
                    body?.string()?.let { json ->
                        JSONObject(json)
                    } ?: JSONObject()
                }
                401,
                403 -> {
                    val message = response.errorBody()?.string()
                        ?.let { parseErrorNetworkResponse(it) }?.userMessage
                        ?: response.message() ?: ""
                    val error = AuthFailureException(message, code)
                    remoteServiceErrorHandler.handleError(error, relativePath)
                    throw error
                }
                in 400..599 -> {
                    val remoteError =
                        response.errorBody()?.string()?.let { parseErrorNetworkResponse(it) }
                    val error = RemoteServiceError(
                        code,
                        remoteError?.userMessage ?: response.message() ?: "",
                        remoteError?.code,
                        remoteError?.name
                    )
                    remoteServiceErrorHandler.handleError(error, relativePath)
                    throw error
                }
                else -> {
                    val error = RuntimeException("Unexpected response $response")
                    remoteServiceErrorHandler.handleError(error, relativePath)
                    throw error
                }
            }
        } ?: throw RuntimeException("Unexpected response $response")
    }

    private fun parseErrorNetworkResponse(json: String): RemoteError? {
        var errorInfo: RemoteError? = null
        try {
            val jsonObject = JSONObject(json)

            errorInfo =
                RemoteError(
                    userMessage = remoteServiceErrorHandler.getUserMessage(jsonObject),
                    code = remoteServiceErrorHandler.getCode(jsonObject),
                    name = remoteServiceErrorHandler.getErrorName(jsonObject)
                )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return errorInfo
    }

    private fun wrap(map: Map<String, Any>): Map<String, Any> {
        val wrapped = hashMapOf<String, Any>()
        map.entries.forEach { entry ->
            wrapped[entry.key] = wrap(entry.value)
        }
        return wrapped
    }

    private fun wrap(any: Any): Any {
        return when (any) {
            JSONObject.NULL -> any
            is JSONArray, is JSONObject -> any
            is Collection<*> -> wrapCollection(any)
            is Map<*, *> -> wrapMap(any)
            is Boolean, is Byte, is Char, is Double, is Float, is Int, is Long, is Short, is String -> any
            any.javaClass.isArray -> JSONArray(any)
            else -> any.toString()
        }
    }

    private fun wrapCollection(collection: Collection<*>): Any {
        val list = mutableListOf<Any>()
        collection.forEach {
            if (it != null) {
                list.add(wrap(it))
            }
        }
        return JSONArray(list)
    }

    private fun wrapMap(map: Map<*, *>): Any {
        val wrapped = hashMapOf<String, Any>()
        map.entries.forEach { entry ->
            val value = entry.value
            if (value != null) {
                wrapped[entry.key as String] = wrap(value)
            }
        }
        return JSONObject(wrapped as Map<*, *>)
    }

    data class RemoteError(
        val userMessage: String? = null,
        val code: String? = null,
        val name: String? = null
    )
}
