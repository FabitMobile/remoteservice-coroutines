package ru.fabit.remoteservicecoroutines.internal

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.QueryMap
import retrofit2.http.Url

internal interface RetrofitApi {
    @GET
    suspend fun getObject(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @QueryMap queries: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @PUT
    suspend fun putObject(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @POST
    suspend fun postObject(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @DELETE
    suspend fun deleteObject(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @QueryMap queries: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @PATCH
    suspend fun patchObject(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body body: RequestBody
    ): Response<ResponseBody>
}