package ru.fabit.remoteservicecoroutines.factories

import okhttp3.OkHttpClient
import retrofit2.Retrofit

class RetrofitFactory constructor(
    private val client: OkHttpClient
) {
    fun getRetrofitBuilder(): Retrofit.Builder {
        return Retrofit.Builder()
            .client(client)
    }
}
