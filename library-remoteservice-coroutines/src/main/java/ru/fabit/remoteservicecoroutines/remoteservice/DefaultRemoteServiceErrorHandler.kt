package ru.fabit.remoteservicecoroutines.remoteservice

import org.json.JSONException
import org.json.JSONObject

class DefaultRemoteServiceErrorHandler : RemoteServiceErrorHandler {
    override fun getUserMessage(jsonObject: JSONObject): String {
        var userMessage = ""
        try {
            userMessage = jsonObject.getString("userMessage")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return userMessage
    }

    override fun getErrorName(jsonObject: JSONObject): String {
        var errorName = ""
        try {
            errorName = jsonObject.getString("errorName")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return errorName
    }

    override fun getCode(jsonObject: JSONObject): String {
        return ""
    }

    override fun handleError(throwable: Throwable, requestPath: String?) {
        //do nothing
    }
}