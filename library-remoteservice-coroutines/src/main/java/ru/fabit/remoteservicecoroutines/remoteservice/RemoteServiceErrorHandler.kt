package ru.fabit.remoteservicecoroutines.remoteservice

import org.json.JSONObject

interface RemoteServiceErrorHandler {
    fun getUserMessage(jsonObject: JSONObject): String
    fun getCode(jsonObject: JSONObject): String
    fun getErrorName(jsonObject: JSONObject): String
    fun handleError(throwable: Throwable, requestPath: String?)
}