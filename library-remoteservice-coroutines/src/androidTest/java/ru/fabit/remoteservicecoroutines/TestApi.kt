package ru.fabit.remoteservicecoroutines

import android.os.Handler
import android.os.Looper
import junit.framework.TestCase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Test
import ru.fabit.error.AuthFailureException
import ru.fabit.error.RemoteServiceError
import ru.fabit.remoteservicecoroutines.entities.RequestMethods
import ru.fabit.remoteservicecoroutines.factories.ClientFactory
import ru.fabit.remoteservicecoroutines.factories.RetrofitFactory
import ru.fabit.remoteservicecoroutines.remoteservice.DefaultRemoteServiceErrorHandler
import ru.fabit.remoteservicecoroutines.remoteservice.RemoteService
import ru.fabit.remoteservicecoroutines.remoteservice.RemoteService.Companion.DEFAULT_KEY
import ru.fabit.remoteservicecoroutines.remoteservice.RemoteServiceConfig
import ru.fabit.remoteservicecoroutines.remoteservice.RemoteServiceImpl
import kotlin.time.Duration.Companion.seconds

class TestApi {
    val config = object : RemoteServiceConfig {
        override val baseUrl = "https://jsonplaceholder.typicode.com"
        override val defaultHeaders = mapOf("content-type" to "application/json; charset=utf-8")
        override val connectTimeoutMillis = 5.seconds.inWholeMilliseconds
        override val readTimeoutMillis = 5.seconds.inWholeMilliseconds
        override val isLogEnabled = true
    }

    val remoteService: RemoteService = RemoteServiceImpl(
        RetrofitFactory(
            ClientFactory().create(
                config,
                okhttp3.Authenticator.NONE
            )
        ).getRetrofitBuilder(),
        config,
        DefaultRemoteServiceErrorHandler()
    )

    @Test
    fun test_GET_JsonArray() = runBlocking {
        val remoteObject = remoteService.getRemoteJson(
            requestMethod = RequestMethods.GET,
            relativePath = "/posts"
        )
        val list = remoteObject.getJSONArray(DEFAULT_KEY)
        assertEquals(100, list.length())
    }

    @Test
    fun test_GET_JsonObject() = runBlocking {
        val id = 10
        val remoteObject = remoteService.getRemoteJson(
            requestMethod = RequestMethods.GET,
            relativePath = "/posts/$id"
        )
        val remoteId = remoteObject.getInt("id")
        assertEquals(id, remoteId)
    }

    @Test
    fun test_POST_JsonObject() = runBlocking {
        val newTitle = "titleText"
        val params = mutableMapOf<String, Any>(
            "userId" to 123,
            "title" to newTitle,
            "body" to "bodyText"
        )
        val newId = 101
        val remoteObject = remoteService.getRemoteJson(
            requestMethod = RequestMethods.POST,
            relativePath = "/posts",
            params = params
        )
        val remoteId = remoteObject.getInt("id")
        val remoteTitle = remoteObject.getString("title")
        assertEquals(newId, remoteId)
        assertEquals(newTitle, remoteTitle)
    }

    @Test
    fun test_PUT_JsonObject() = runBlocking {
        val newTitle = "titleText"
        val id = 1
        val params = mutableMapOf<String, Any>(
            "id" to id,
            "userId" to 123,
            "title" to newTitle
        )
        val remoteObject = remoteService.getRemoteJson(
            requestMethod = RequestMethods.PUT,
            relativePath = "/posts/$id",
            params = params
        )
        val remoteId = remoteObject.getInt("id")
        val remoteTitle = remoteObject.getString("title")
        assertEquals(id, remoteId)
        assertEquals(newTitle, remoteTitle)
    }

    @Test
    fun test_PATCH_JsonObject() = runBlocking {
        val newTitle = "titleText"
        val id = 1
        val params = mutableMapOf<String, Any>(
            "title" to newTitle
        )
        val remoteObject = remoteService.getRemoteJson(
            requestMethod = RequestMethods.PATCH,
            relativePath = "/posts/$id",
            params = params
        )
        val remoteId = remoteObject.getInt("id")
        val remoteTitle = remoteObject.getString("title")
        assertEquals(id, remoteId)
        assertEquals(newTitle, remoteTitle)
    }

    @Test
    fun test_DELETE_JsonObject() = runBlocking {
        val id = 1
        val remoteObject = remoteService.getRemoteJson(
            requestMethod = RequestMethods.DELETE,
            relativePath = "/posts/$id"
        )
        assertEquals(JSONObject().toString(), remoteObject.toString())
    }

    @Test
    fun test_404(): Unit = runBlocking {
        try {
            remoteService.getRemoteJson(
                requestMethod = RequestMethods.GET,
                relativePath = "/notfound"
            )
        } catch (e: RemoteServiceError) {
            assertEquals(404, e.errorCode)
        } catch (e: Exception) {
            throw AssertionError(e)
        }
    }

    @Test(expected = AuthFailureException::class)
    fun test_401(): Unit = runBlocking {
        val userId = "321"
        remoteService.getRemoteJson(
            requestMethod = RequestMethods.GET,
            baseUrl = "https://gmail.googleapis.com",
            relativePath = "/gmail/v1/users/${userId}/messages"
        )
    }

    @Test
    fun test_Main_thread() {
        var t: Throwable? = null
        Handler(Looper.getMainLooper()).post {
            try {
                runBlocking {
                    remoteService.getRemoteJson(
                        requestMethod = RequestMethods.GET,
                        relativePath = "/get"
                    )
                }
            } catch (e: IllegalThreadStateException) {
                t = e
            }
        }
        runBlocking {
            awaitAll(CoroutineScope(Dispatchers.IO).async {
                Thread.sleep(1000)
            })
        }
        assertNotNull(t)
        assertEquals(IllegalThreadStateException::class, t!!::class)
    }
}