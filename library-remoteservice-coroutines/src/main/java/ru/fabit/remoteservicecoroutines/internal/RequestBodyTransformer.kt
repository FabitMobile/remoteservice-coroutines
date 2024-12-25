package ru.fabit.remoteservicecoroutines.internal

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

internal class RequestBodyTransformer {

    private val contentType by lazy { "application/json; charset=utf-8".toMediaTypeOrNull() }

    fun getRequestBody(params: Map<String, Any>?): RequestBody {
        val wrappedParams = if (params != null) {
            wrap(params)
        } else {
            mapOf<Any, Any>()
        }
        val jsonObject = JSONObject(wrappedParams)
        return jsonObject.toString().toRequestBody(contentType)
    }

    private fun wrap(map: Map<String, Any>): Map<String, Any> {
        val wrapped = hashMapOf<String, Any>()
        map.entries.forEach { entry ->
            wrapped[entry.key] = wrap(entry.value)
        }
        return wrapped
    }

    private fun wrap(any: Any): Any {
        return when (any) {
            JSONObject.NULL -> any
            is JSONArray, is JSONObject -> any
            is Collection<*> -> wrapCollection(any)
            is Map<*, *> -> wrapMap(any)
            is Boolean, is Byte, is Char, is Double, is Float, is Int, is Long, is Short, is String -> any
            any.javaClass.isArray -> JSONArray(any)
            else -> any.toString()
        }
    }

    private fun wrapCollection(collection: Collection<*>): Any {
        val list = mutableListOf<Any>()
        collection.forEach {
            if (it != null) {
                list.add(wrap(it))
            }
        }
        return JSONArray(list)
    }

    private fun wrapMap(map: Map<*, *>): Any {
        val wrapped = hashMapOf<String, Any>()
        map.entries.forEach { entry ->
            val value = entry.value
            if (value != null) {
                wrapped[entry.key as String] = wrap(value)
            }
        }
        return JSONObject(wrapped as Map<*, *>)
    }
}