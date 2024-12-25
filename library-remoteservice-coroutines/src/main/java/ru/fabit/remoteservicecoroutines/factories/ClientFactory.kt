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

open class ClientFactory {

    open fun create(
        remoteServiceConfig: RemoteServiceConfig,
        authenticator: Authenticator? = null,
        vararg interceptors: Interceptor
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
        builder.setTimeouts(
            connectTimeoutMillis = remoteServiceConfig.connectTimeoutMillis,
            readTimeoutMillis = remoteServiceConfig.readTimeoutMillis
        )
        if (remoteServiceConfig.isLogEnabled)
            getLoggingInterceptor()?.let { builder.addInterceptor(it) }

        getDefaultHeadersInterceptor(remoteServiceConfig.defaultHeaders.toHeaders())?.let {
            builder.addInterceptor(it)
        }
        interceptors.forEach(builder::addInterceptor)

        authenticator?.let { builder.authenticator(it) }
        return builder.build()
    }

    protected open fun OkHttpClient.Builder.setTimeouts(
        connectTimeoutMillis: Long,
        readTimeoutMillis: Long
    ): OkHttpClient.Builder {
        return this.apply {
            connectTimeout(connectTimeoutMillis, TimeUnit.MILLISECONDS)
            readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
        }
    }

    protected open fun getDefaultHeadersInterceptor(headers: Headers): Interceptor? =
        Interceptor { chain ->
            var request = chain.request()
            val requestBuilder = request.newBuilder()
            val includedHeaders = request.headers
            val newHeaders = includedHeaders.newBuilder()
            for (key in headers.names()) {
                if (includedHeaders[key] == null) {
                    newHeaders.add(key, headers[key] ?: "")
                }
            }
            request = requestBuilder
                .headers(newHeaders.build())
                .build()
            chain.proceed(request)
        }

    protected open fun getLoggingInterceptor(): LoggingInterceptor? {
        return LoggingInterceptor.Builder()
            .setLevel(Level.BASIC)
            .request("Request")
            .response("Response")
            .build()
    }
}