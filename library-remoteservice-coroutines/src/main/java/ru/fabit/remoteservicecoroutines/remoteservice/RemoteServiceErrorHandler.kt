package ru.fabit.remoteservicecoroutines.remoteservice

import org.json.JSONObject
import ru.fabit.error.RemoteServiceError

interface RemoteServiceErrorHandler {
    fun getUserMessage(jsonObject: JSONObject): String
    fun getCode(jsonObject: JSONObject): String
    fun getErrorName(jsonObject: JSONObject): String
    fun handleError(throwable: Throwable, requestPath: String?)

    fun getError(code: Int, body: String?): RemoteServiceError? {
        return try {
            val jsonObject = JSONObject(body ?: return null)
            RemoteServiceError(
                errorCode = code,
                detailMessage = getUserMessage(jsonObject),
                code = getCode(jsonObject),
                errorName = getErrorName(jsonObject)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}