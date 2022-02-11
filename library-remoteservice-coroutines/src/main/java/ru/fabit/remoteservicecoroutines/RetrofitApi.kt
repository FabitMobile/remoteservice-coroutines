package ru.fabit.remoteservicecoroutines

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface RetrofitApi {

    @GET
    fun getObject(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @QueryMap queries: Map<String, @JvmSuppressWildcards Any>
    ): Call<ResponseBody>

    @PUT
    fun putObject(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body body: RequestBody
    ): Call<ResponseBody>

    @POST
    fun postObject(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body body: RequestBody
    ): Call<ResponseBody>

    @DELETE
    fun deleteObject(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @QueryMap queries: Map<String, @JvmSuppressWildcards Any>
    ): Call<ResponseBody>

    @PATCH
    fun patchObject(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body body: RequestBody
    ): Call<ResponseBody>
}