package ru.fabit.remoteservicecoroutines.remoteservice

import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.*
import ru.fabit.error.AuthFailureException
import ru.fabit.error.NoNetworkConnectionException
import ru.fabit.error.RemoteServiceError
import ru.fabit.error.RequestTimeoutError
import ru.fabit.remoteservicecoroutines.RetrofitApi
import ru.fabit.remoteservicecoroutines.entities.RequestMethods
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
        params: HashMap<String, Any>,
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
        params: HashMap<String, Any>,
        headers: Map<String, String>
    ): JSONObject {
        return getRemoteJsonInner(requestMethod, baseUrl, relativePath, params, headers)
    }

    private suspend fun getRemoteJsonInner(
        requestMethod: Int,
        baseUrl: String,
        relativePath: String,
        params: HashMap<String, Any>,
        headers: Map<String, String>
    ): JSONObject = suspendCancellableCoroutine { continuation ->
            if (Looper.getMainLooper() == Looper.myLooper()) throw IllegalThreadStateException()
            val url = baseUrl.plus(relativePath)
            val call = when (requestMethod) {
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
        call.enqueue(object : Callback<ResponseBody>{
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                try {
                    val jsonObject = mapResponseToJSONObject(response, relativePath)
                    continuation.resume(jsonObject)
                } catch (t: Throwable) {
                    val throwable = onError(t)
                    continuation.resumeWithException(throwable)
                }
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                val throwable = onError(t)
                continuation.resumeWithException(throwable)
            }

        })
        continuation.invokeOnCancellation { call.cancel() }
    }

    private fun getRequestBody(params: HashMap<String, Any>?): RequestBody {
        val wrappedParams = if (params != null) {
            wrap(params).toMap()
        } else {
            mapOf<Any, Any>()
        }
        val jsonObject = JSONObject(wrappedParams)

        return RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            jsonObject.toString()
        )
    }

    private fun onError(t: Throwable): Throwable {
        return when (t) {
            is SocketTimeoutException -> RequestTimeoutError(t.message)
            is IllegalThreadStateException -> IllegalThreadStateException(t.message)
            is IOException -> NoNetworkConnectionException(t.message)
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

    private fun wrap(map: HashMap<String, Any>): HashMap<String, Any> {
        val wrapped = hashMapOf<String, Any>()
        map.entries.forEach { entry ->
            wrapped.put(entry.key, wrap(entry.value))
        }
        return wrapped
    }

    private fun wrap(any: Any): Any {
        return when (any) {
            JSONObject.NULL -> any
            is JSONArray, is JSONObject -> any
            is Collection<*> -> wrapCollection(any)
            any.javaClass.isArray -> JSONArray(any)
            is Map<*, *> -> wrapMap(any)
            is Boolean, is Byte, is Char, is Double, is Float, is Int, is Long, is Short, is String -> any
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
                wrapped.put(entry.key as String, wrap(value))
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
