package ru.fabit.remoteservicecoroutines

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface RetrofitApi {

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