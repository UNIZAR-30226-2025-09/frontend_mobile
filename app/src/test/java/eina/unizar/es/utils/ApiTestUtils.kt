package eina.unizar.es.utils

import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class ApiTestUtils {
    private val client = OkHttpClient.Builder()
        .connectTimeout(TestConfig.TIMEOUT, TimeUnit.MILLISECONDS)
        .readTimeout(TestConfig.TIMEOUT, TimeUnit.MILLISECONDS)
        .build()

    companion object {
        private var authToken: String? = null
        private var userId: Int? = null

        fun setAuthToken(token: String) {
            authToken = token
        }

        fun getAuthToken(): String? = authToken

        fun setUserId(id: Int) {
            userId = id
        }

        fun getUserId(): Int? = userId
    }

    // Función para realizar login y obtener token
    suspend fun login(email: String = TestConfig.TEST_EMAIL, password: String = TestConfig.TEST_PASSWORD): Boolean {
        val jsonBody = JSONObject().apply {
            put("email", email)
            put("password", password)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${TestConfig.BASE_URL}/api/auth/login")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val body = response.body?.string()
                val jsonResponse = JSONObject(body!!)
                setAuthToken(jsonResponse.getString("token"))
                setUserId(jsonResponse.getJSONObject("user").getInt("id"))
                return true
            }
        }
        return false
    }

    // Función para GET con autenticación
    fun get(endpoint: String): Pair<Int, Any?> {
        val request = Request.Builder()
            .url("${TestConfig.BASE_URL}$endpoint")
            .header("Authorization", "Bearer ${getAuthToken()}")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            return Pair(
                response.code,
                when {
                    body.isNullOrEmpty() -> null
                    body.trim().startsWith("[") -> JSONArray(body)  // Para arrays directos
                    body.trim().startsWith("{") -> JSONObject(body)  // Para objetos
                    else -> throw JSONException("Invalid JSON response")
                }
            )
        }
    }

    // Función para POST con autenticación
    fun post(endpoint: String, jsonBody: JSONObject): Pair<Int, JSONObject?> {
        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${TestConfig.BASE_URL}$endpoint")
            .header("Authorization", "Bearer ${getAuthToken()}")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            return Pair(
                response.code,
                if (body.isNullOrEmpty()) null else JSONObject(body)
            )
        }
    }

    // Función para PUT con autenticación
    fun put(endpoint: String, jsonBody: JSONObject): Pair<Int, JSONObject?> {
        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${TestConfig.BASE_URL}$endpoint")
            .header("Authorization", "Bearer ${getAuthToken()}")
            .put(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            return Pair(
                response.code,
                if (body.isNullOrEmpty()) null else JSONObject(body)
            )
        }
    }

    // Función para DELETE con autenticación
    fun delete(endpoint: String): Pair<Int, JSONObject?> {
        val request = Request.Builder()
            .url("${TestConfig.BASE_URL}$endpoint")
            .header("Authorization", "Bearer ${getAuthToken()}")
            .delete()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            return Pair(
                response.code,
                if (body.isNullOrEmpty()) null else JSONObject(body)
            )
        }
    }
}