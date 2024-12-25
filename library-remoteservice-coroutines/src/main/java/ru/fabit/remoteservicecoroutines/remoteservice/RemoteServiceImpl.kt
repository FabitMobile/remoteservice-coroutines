package ru.fabit.remoteservicecoroutines.remoteservice

import android.os.Looper
import kotlinx.coroutines.CancellationException
import okhttp3.ResponseBody
import org.json.JSONObject
import org.json.JSONTokener
import retrofit2.Response
import retrofit2.Retrofit
import ru.fabit.error.AuthFailureException
import ru.fabit.error.NoNetworkConnectionException
import ru.fabit.error.RemoteServiceError
import ru.fabit.error.RequestTimeoutError
import ru.fabit.remoteservicecoroutines.entities.RequestMethods
import ru.fabit.remoteservicecoroutines.internal.RequestBodyTransformer
import ru.fabit.remoteservicecoroutines.internal.RetrofitApi
import ru.fabit.remoteservicecoroutines.remoteservice.RemoteService.Companion.DEFAULT_KEY
import java.io.IOException
import java.net.SocketTimeoutException

class RemoteServiceImpl(
    private val retrofitBuilder: Retrofit.Builder,
    private val remoteServiceConfig: RemoteServiceConfig,
    private val remoteServiceErrorHandler: RemoteServiceErrorHandler
) : RemoteService {

    private val requestBodyTransformer = RequestBodyTransformer()

    private val retrofit by lazy {
        retrofitBuilder
            .baseUrl(remoteServiceConfig.baseUrl)
            .build()
    }
    private val api by lazy { retrofit.create(RetrofitApi::class.java) }

    override suspend fun getRemoteJson(
        requestMethod: Int,
        relativePath: String,
        baseUrl: String?,
        params: Map<String, Any>,
        headers: Map<String, String>
    ): JSONObject {
        return getRemoteJsonInner(
            requestMethod,
            baseUrl ?: remoteServiceConfig.baseUrl,
            relativePath,
            params,
            headers
        )
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
        if (Looper.getMainLooper() == Looper.myLooper())
            throw IllegalThreadStateException()
        val url = baseUrl.plus(relativePath)
        val body = requestBodyTransformer.getRequestBody(params)
        try {
            val response = when (requestMethod) {
                RequestMethods.GET -> api.getObject(url, headers, params)
                RequestMethods.PUT -> api.putObject(url, headers, body)
                RequestMethods.POST -> api.postObject(url, headers, body)
                RequestMethods.DELETE -> api.deleteObject(url, headers, params)
                RequestMethods.PATCH -> api.patchObject(url, headers, body)

                else -> throw Throwable("No requestMethod")
            }
            jsonObject = mapResponseToJSONObject(response, relativePath)
        } catch (t: Throwable) {
            val throwable = onError(t)
            throw throwable
        }
        return jsonObject
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
                        when (val item = JSONTokener(json).nextValue()) {
                            is JSONObject -> item
                            else -> JSONObject().apply {
                                put(DEFAULT_KEY, item)
                            }
                        }
                    } ?: JSONObject()
                }

                401,
                403 -> {
                    val remoteError =
                        remoteServiceErrorHandler.getError(code, response.errorBody()?.string())
                    val message = remoteError?.detailMessage ?: response.message() ?: ""
                    val error = AuthFailureException(message, code)
                    remoteServiceErrorHandler.handleError(error, relativePath)
                    throw error
                }

                in 400..599 -> {
                    val remoteError =
                        remoteServiceErrorHandler.getError(code, response.errorBody()?.string())
                    val error = remoteError ?: RemoteServiceError(
                        errorCode = code,
                        detailMessage = response.message()
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
}
