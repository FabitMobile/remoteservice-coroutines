package ru.fabit.remoteservicecoroutines.factories

import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import okhttp3.Authenticator
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import ru.fabit.remoteservicecoroutines.remoteservice.RemoteServiceConfig
import java.util.concurrent.TimeUnit

class ClientFactory {

    fun create(
        remoteServiceConfig: RemoteServiceConfig,
        authenticator: Authenticator
    ): OkHttpClient {
        val builder = getPreconfiguredClientBuilder(
            remoteServiceConfig.connectTimeoutMillis,
            remoteServiceConfig.readTimeoutMillis
        )
        addInterceptors(
            builder,
            authenticator,
            remoteServiceConfig.defaultHeaders.toHeaders(),
            remoteServiceConfig.isLogEnabled
        )
        return builder.build()
    }

    private fun getPreconfiguredClientBuilder(
        connectTimeoutMillis: Long,
        readTimeoutMillis: Long
    ): OkHttpClient.Builder {
        return OkHttpClient.Builder().apply {
            connectTimeout(connectTimeoutMillis, TimeUnit.MILLISECONDS)
            readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
        }
    }

    private fun addInterceptors(
        builder: OkHttpClient.Builder,
        authenticator: Authenticator,
        headers: Headers,
        isLogEnabled: Boolean
    ) {
        with(builder) {
            authenticator(authenticator)
            if (isLogEnabled) {
                addInterceptor(getLoggingInterceptor())
            }
            addInterceptor(getInterceptors(headers))
        }
    }

    private fun getInterceptors(headers: Headers) = Interceptor { chain ->
        var request = chain.request()
        val requestBuilder = request.newBuilder()
        val includedHeaders = request.headers
        val newHeaders = includedHeaders.newBuilder()
        for (key in headers.names()) {
            if (includedHeaders.get(key) == null) {
                newHeaders.add(key, headers.get(key) ?: "")
            }
        }
        request = requestBuilder
            .headers(newHeaders.build())
            .build()
        chain.proceed(request)
    }


    private fun getLoggingInterceptor(): LoggingInterceptor {
        return LoggingInterceptor.Builder()
            .setLevel(Level.BASIC)
            .request("Request")
            .response("Response")
            .build()
    }
}