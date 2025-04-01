package eina.unizar.es.data.model.network
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.navigation.NavController
import eina.unizar.es.data.model.network.ApiClient.BASE_URL
import eina.unizar.es.ui.playlist.Playlist
import eina.unizar.es.ui.song.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray

object ApiClient {
    const val BASE_URL = "http://10.0.2.2/request/api" // Usa la IP local del backend
    //const val BASE_URL = "http://164.90.160.181/request/api" // Usa la IP publica (nube) del backend
    //const val BASE_URL_IMG = "http://164.90.160.181/request"
    const val BASE_URL_IMG = "http://10.0.2.2/request"


    /**
     * Método para realizar una petición GET en segundo plano.
     * @param endpoint Ruta del recurso (ejemplo: "playlists").
     * @return Respuesta en formato JSON o `null` si hay error.
     */
    suspend fun get(endpoint: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/$endpoint")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            val response = connection.inputStream.bufferedReader().readText()

            println("Código de respuesta: $responseCode")
            println("Respuesta del servidor: $response")

            return@withContext if (responseCode == HttpURLConnection.HTTP_OK) {
                response
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error al conectar con el backend: ${e.message}")
            null
        }
    }

    /**
     * Método para realizar una petición POST en segundo plano.
     * @param endpoint Ruta del recurso (ejemplo: "create-payment-intent").
     * @param jsonBody Cuerpo de la solicitud en formato JSON.
     * @return Respuesta en formato JSON o `null` si hay error.
     */
    suspend fun post(endpoint: String, jsonBody: JSONObject): String? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/$endpoint")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                // Escribir el cuerpo de la petición
                connection.outputStream.use { os ->
                    os.write(jsonBody.toString().toByteArray())
                    os.flush()
                }

                val responseCode = connection.responseCode
                Log.d("ApiClient", "Código de respuesta: $responseCode")

                return@withContext if (responseCode in 200..299) { // Acepta códigos 2XX
                    connection.inputStream.bufferedReader()
                        .use { it.readText() } // Lee la respuesta correctamente
                } else {
                    Log.e("ApiClient", "Error en la respuesta del servidor: código $responseCode")
                    connection.errorStream?.bufferedReader()
                        ?.use { it.readText() } // Leer el mensaje de error si lo hay
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ApiClient", "Error de conexión con el backend: ${e.message}")
                null
            }
        }


    /*
     * Función para realizar una solicitud HTTP POST con encabezados personalizados.
     *
     * @param endpoint Ruta del recurso en la API (ejemplo: "user/login").
     * @param jsonBody Cuerpo de la solicitud en formato JSON.
     * @param context Contexto de la aplicación, utilizado para acceder a SharedPreferences si es necesario.
     * @param responseHeaders Mapa mutable donde se almacenarán las cabeceras de la respuesta.
     *
     * @return La respuesta del servidor en formato String o `null` en caso de error.
     */

    fun postWithHeaders(
        endpoint: String,
        jsonBody: JSONObject,
        context: Context,
        responseHeaders: MutableMap<String, String>
    ): String? {
        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = jsonBody.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("$BASE_URL/$endpoint") // Para emulador Android
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()

                // Extraer todas las cabeceras
                for ((name, value) in response.headers) {
                    responseHeaders[name] = value
                }

                // Log para verificar si el token está en la cabecera
                val token = responseHeaders["Authorization"]
                if (!token.isNullOrEmpty()) {
                    Log.d("API", "Token recibido: $token")
                } else {
                    Log.e("API", "No se recibió el token en la cabecera")
                }

                if (!response.isSuccessful) {
                    Log.e("API", "Error en la respuesta: código ${response.code}")
                    null
                } else {
                    responseBody
                }
            }
        } catch (e: IOException) {
            Log.e("API", "Error en la petición: ${e.message}", e)
            null
        }
    }


    /**
     * Función para cerrar sesión eliminando el token almacenado.
     */
    suspend fun logoutUser(context: Context, navController: NavController) {
        withContext(Dispatchers.IO) {
            try {
                val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("auth_token", null)

                if (token.isNullOrEmpty()) {
                    Log.e("Logout", "No hay token guardado, no se puede cerrar sesión")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: No has iniciado sesión", Toast.LENGTH_LONG).show()
                    }
                    return@withContext
                }

                val jsonBody = JSONObject()  // No enviamos datos, solo la petición con el token
                val headers = mutableMapOf<String, String>("Authorization" to "Bearer $token")

                val response = ApiClient.postWithHeaders("user/logout", jsonBody, context, headers)

                if (response != null) {
                    // Eliminar el token de SharedPreferences
                    sharedPreferences.edit().remove("auth_token").apply()

                    Log.d("Logout", "Sesión cerrada correctamente")

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_LONG).show()

                        // Navegar al login y limpiar historial de navegación
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                } else {
                    Log.e("Logout", "Error al cerrar sesión: respuesta nula")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error al cerrar sesión", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("LogoutError", "Error cerrando sesión: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error inesperado al cerrar sesión", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


/*
 * Función para obtener datos del usuario
 */
suspend fun getUserData(context: Context): Map<String, Any>? {
    return withContext(Dispatchers.IO) {
        try {
            val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val token = sharedPreferences.getString("auth_token", null) ?: return@withContext null

                val headers = mapOf("Authorization" to "Bearer $token")
                val response = getWithHeaders("user/profile", context, headers)

                if (response != null) {
                    val jsonResponse = JSONObject(response)

                    Log.d("UserData", "Datos recibidos: $jsonResponse") // Debug para ver qué devuelve la API

                    return@withContext mapOf(
                        "id" to jsonResponse.optInt("id", 0),
                        "nickname" to jsonResponse.optString("nickname", ""),
                        "mail" to jsonResponse.optString("mail", ""),
                        "is_premium" to jsonResponse.optBoolean("is_premium", false),
                        "user_picture" to jsonResponse.optString("user_picture", "")
                    )
                }
            } catch (e: Exception) {
                Log.e("UserData", "Error al obtener los datos del usuario", e)
            }
            return@withContext null
        }
    }

/**
 * Realiza una petición GET con encabezados personalizados (ej. `Authorization: Bearer <TOKEN>`).
 *
 * @param endpoint Ruta del recurso en la API (ejemplo: "user/profile").
 * @param context Contexto para acceder a SharedPreferences.
 * @param headers Mapa con las cabeceras HTTP a incluir en la petición.
 * @return La respuesta en formato String o `null` si hay error.
 */
suspend fun getWithHeaders(endpoint: String, context: Context, headers: Map<String, String>): String? {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val requestBuilder = Request.Builder()
                .url("$BASE_URL/$endpoint")
                .get()

                // Agregar cabeceras
                headers.forEach { (key, value) ->
                    requestBuilder.addHeader(key, value)
                }

                val request = requestBuilder.build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e("API", "Error en GET $endpoint: código ${response.code}")
                    return@withContext null
                }

                response.body?.string()
            } catch (e: IOException) {
                Log.e("API", "Error en la petición GET: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Realiza una petición PUT con encabezados personalizados (ej. `Authorization: Bearer <TOKEN>`).
     *
     * @param endpoint Ruta del recurso en la API (ejemplo: "user/update").
     * @param jsonBody Cuerpo de la solicitud en formato JSON.
     * @param context Contexto para acceder a SharedPreferences.
     * @param headers Mapa con las cabeceras HTTP a incluir en la petición.
     * @return La respuesta en formato String o `null` si hay error.
     */
    suspend fun putWithHeaders(endpoint: String, jsonBody: JSONObject, context: Context, headers: Map<String, String>): String? {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = jsonBody.toString().toRequestBody(mediaType)

                val requestBuilder = Request.Builder()
                    .url("$BASE_URL/$endpoint")
                    .put(body)

                // Agregar cabeceras
                headers.forEach { (key, value) ->
                    requestBuilder.addHeader(key, value)
                }

                val request = requestBuilder.build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e("API", "Error en PUT $endpoint: código ${response.code}")
                    return@withContext null
                }

                response.body?.string()
            } catch (e: IOException) {
                Log.e("API", "Error en la petición PUT: ${e.message}", e)
                null
            }
        }
    }

/**
 * Realiza una petición HTTP POST al servidor para actualizar el estado de `is_premium` del usuario autenticado.
 *
 * **Función**: Envía una solicitud al endpoint `user/premium` para cambiar el estado de suscripción del usuario.
 * **Autenticación**: Se obtiene el **token JWT** desde `SharedPreferences` y se envía en la cabecera `Authorization`.
 * **Manejo de errores**:
 *   - Si no hay token disponible, se muestra un error en el log y la función devuelve `null`.
 *   - Si la petición falla, se captura el error y se muestra en el log.
 *   - Si la respuesta es inválida (`401 Unauthorized` o similar), devuelve `null`.
 *
 * @param endpoint Endpoint de la API (ejemplo: `"user/premium"`).
 * @param jsonBody Cuerpo de la solicitud en formato JSON.
 * @param context Contexto para obtener SharedPreferences.
 * @return Respuesta del servidor en formato `String`, o `null` en caso de error.
 */
suspend fun postTokenPremium(
    endpoint: String,
    jsonBody: JSONObject,
    context: Context
): String? = withContext(Dispatchers.IO) {
    try {
        // Obtener el token desde SharedPreferences
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val token = sharedPreferences.getString("auth_token", null)

            if (token.isNullOrEmpty()) {
                Log.e("API", "Token no disponible")
                return@withContext null
            }

            val client = OkHttpClient()
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = jsonBody.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL/$endpoint") // Construcción de la URL
                .post(body) // Método POST
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $token") // Se envía el token aquí
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    Log.e("API", "Error en la respuesta: código ${response.code}, mensaje: ${responseBody}")
                    return@withContext null
                } else {
                    Log.d("API", "Respuesta exitosa: $responseBody")
                    return@withContext responseBody
                }
            }
        } catch (e: IOException) {
            Log.e("API", "Error en la petición: ${e.message}", e)
            return@withContext null
        }
    }


    suspend fun likeUnlikePlaylist(playlistId: String, userId : String, isLiked: Boolean): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (userId.isNullOrEmpty()) {
                    // Si no se obtiene el user_id, muestra un mensaje de error o realiza alguna acción.
                    println("Error: user_id no encontrado")
                } else {
                    println("user_id: $userId")  // Verifica que el user_id es correcto
                }

                val url = URL("$BASE_URL/playlists/$playlistId/like")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("user_id", userId)
                }

                connection.outputStream.write(jsonBody.toString().toByteArray())
                connection.connect()

                val responseCode = connection.responseCode
                val responseMessage = connection.inputStream.bufferedReader().readText()

                println("Código de respuesta: $responseCode")
                println("Mensaje de respuesta: $responseMessage")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    responseMessage // Si todo fue bien, devolver la respuesta del servidor
                } else {
                    null // Si algo salió mal, devolver null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null // En caso de error, devolver null
            }
        }
    }

    suspend fun getLikedPlaylists(userId: String): List<Playlist>? {
        return withContext(Dispatchers.IO) {
            try {
                if (userId.isNullOrEmpty()) {
                    // Si no se obtiene el user_id, muestra un mensaje de error o realiza alguna acción.
                    println("Error: user_id no encontrado")
                } else {
                    println("user_id: $userId")  // Verifica que el user_id es correcto
                }

                // Realiza la solicitud GET para obtener las playlists que un usuario ha dado like
                val url = URL("$BASE_URL/playlists/liked/$userId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")

                val responseCode = connection.responseCode
                val response = connection.inputStream.bufferedReader().readText()

                // Verificamos que la respuesta sea correcta
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val jsonArray = JSONArray(response)
                    val likedPlaylists = mutableListOf<Playlist>()

                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        likedPlaylists.add(
                            Playlist(
                                id = jsonObject.getString("id"),
                                title = jsonObject.getString("name"),
                                idAutor = jsonObject.getString("user_id"),
                                idArtista = jsonObject.getString("artist_id"),
                                description = jsonObject.getString("description"),
                                esPublica = jsonObject.getString("type"),
                                esAlbum = jsonObject.getString("typeP"),
                                imageUrl = jsonObject.getString("front_page")
                            )
                        )
                    }
                    return@withContext likedPlaylists
                } else {
                    null // En caso de que haya error en la respuesta
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null // En caso de error
            }
        }
    }

    suspend fun likeUnlikeSong(songId: String, userId: String, isLiked: Boolean): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (userId.isNullOrEmpty()) {
                    // Si no se obtiene el user_id, muestra un mensaje de error o realiza alguna acción.
                    println("Error: user_id no encontrado")
                    return@withContext null
                } else {
                    println("user_id: $userId")  // Verifica que el user_id es correcto
                }

                val url = URL("$BASE_URL/song_like/$songId/likeUnlike")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("user_id", userId)
                }

                connection.outputStream.write(jsonBody.toString().toByteArray())
                connection.connect()

                val responseCode = connection.responseCode
                val responseMessage = connection.inputStream.bufferedReader().readText()

                println("Código de respuesta: $responseCode")
                println("Mensaje de respuesta: $responseMessage")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    responseMessage // Si todo fue bien, devolver la respuesta del servidor
                } else {
                    null // Si algo salió mal, devolver null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null // En caso de error, devolver null
            }
        }
    }

    suspend fun getLikedSongsPlaylist(userId: String): List<Song>? {
        return withContext(Dispatchers.IO) {
            try {
                if (userId.isBlank()) {
                    Log.e("getLikedSongsPlaylist", "User ID is empty")
                    return@withContext null
                }

                // Use your actual base URL
                val url = URL("$BASE_URL/song_like/$userId/likedSongs")

                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 10000 // 10 seconds
                    readTimeout = 10000 // 10 seconds
                }

                when (val responseCode = connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        Log.d("getLikedSongsPlaylist", "Response: $response")

                        val jsonArray = JSONArray(response)
                        return@withContext (0 until jsonArray.length()).map { i ->
                            val jsonObject = jsonArray.getJSONObject(i)
                            Song(
                                id = jsonObject.getInt("id"),
                                name = jsonObject.getString("name"),
                                duration = jsonObject.getInt("duration"),
                                letra = jsonObject.optString("lyrics", ""),
                                photo_video = jsonObject.optString("photo_video", ""),
                                url_mp3 = jsonObject.optString("url_mp3", "")
                            )
                        }
                    }
                    else -> {
                        Log.e("getLikedSongsPlaylist", "Error response code: $responseCode")
                        val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e("getLikedSongsPlaylist", "Error response: $errorResponse")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("getLikedSongsPlaylist", "Exception occurred", e)
                null
            }
        }
    }

    /**
     * Método para realizar una petición DELETE en segundo plano.
     * @param endpoint Ruta del recurso a eliminar (ejemplo: "playlists/123").
     * @return Respuesta del servidor en formato String o `null` si hay error.
     */
    suspend fun delete(endpoint: String): String? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("$BASE_URL/$endpoint")
                .delete() // Usamos el método DELETE
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("API", "Error en DELETE $endpoint: código ${response.code}")
                    return@withContext null
                }

                response.body?.string()
            }
        } catch (e: IOException) {
            Log.e("API", "Error en la petición DELETE: ${e.message}", e)
            null
        }
    }

    /**
     * El método getImageUrl toma un path (ruta) opcional de una imagen y devuelve una URL
     * completa para esa imagen. Su objetivo es manejar diferentes tipos de rutas de imagen
     * (relativas, absolutas o nulas) y asegurar que siempre se devuelva una URL válida.
     */
    fun getImageUrl(path: String?, fallback: String = "default.jpg"): String {
        val pathToUse = if (path.isNullOrEmpty()) fallback else path
        Log.d("Getimg", "Path a usar: $pathToUse")

        return when {
            pathToUse.startsWith("http") -> pathToUse
            else -> {
                val cleanPath = pathToUse.replace(Regex("^/?"), "")
                val finalUrl = "$BASE_URL_IMG/$cleanPath"
                Log.d("Getimg", "URL final: $finalUrl")
                finalUrl
            }
        }
    }
}