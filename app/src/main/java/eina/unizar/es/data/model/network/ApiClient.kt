package eina.unizar.es.data.model.network
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.navigation.NavController
import eina.unizar.es.ui.playlist.Playlist
import eina.unizar.es.ui.song.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

object ApiClient {
    //const val BASE_URL = "http://10.0.2.2/request/api" // Usa la IP local del backend
    //const val BASE_URL_IMG = "http://10.0.2.2/request"
    const val BASE_URL = "http://164.90.160.181/request/api" // Usa la IP publica (nube) del backend
    const val BASE_URL_IMG = "http://164.90.160.181/request"



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

    /**
     * Método para realizar una petición POST en segundo plano.
     * @param endpoint Ruta del recurso (ejemplo: "create-payment-intent").
     * @param jsonBody Cuerpo de la solicitud en formato JSON.
     * @return Respuesta en formato JSON o `null` si hay error y código.
     */
    suspend fun postWithCode(endpoint: String, jsonBody: JSONObject): Pair<Int, String?> =
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

                val responseBody = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    Log.e("ApiClient", "Error en la respuesta del servidor: código $responseCode")
                    connection.errorStream?.bufferedReader()?.use { it.readText() }
                }

                Pair(responseCode, responseBody)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ApiClient", "Error de conexión con el backend: ${e.message}")
                Pair(500, null) // Devuelve código 500 para errores de conexión
            }
        }

    /**
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
     * Performs a user logout by making an API request to invalidate the session token.
     *
     * This function:
     * 1. Retrieves the authentication token from SharedPreferences
     * 2. Makes a POST request to the logout endpoint with the token
     * 3. Clears the stored token upon successful logout
     * 4. Navigates back to the login screen
     * 5. Displays appropriate Toast messages for success or failure
     *
     * @param context The application context used to access SharedPreferences and display Toast messages
     * @param navController The NavController used to navigate to the login screen after logout
     *
     * @throws Exception If there's an error during the network request or token processing
     */
    suspend fun logoutUser(context: Context, navController: NavController) {
        withContext(Dispatchers.IO) {
            try {
                val sharedPreferences =
                    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("auth_token", null)

                if (token.isNullOrEmpty()) {
                    Log.e("Logout", "No hay token guardado, no se puede cerrar sesión")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: No has iniciado sesión", Toast.LENGTH_LONG)
                            .show()
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
                        Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_LONG)
                            .show()

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
                    Toast.makeText(context, "Error inesperado al cerrar sesión", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }


    /**
     * Retrieves the current user's profile data from the server.
     *
     * This function makes an authenticated GET request to fetch the user profile data,
     * including user ID, nickname, email, premium status, and profile picture.
     *
     * @param context The application context used to access SharedPreferences and make API requests
     * @return A Map containing user profile data with the following keys:
     *         - "id": The user's ID (Int)
     *         - "nickname": The user's display name (String)
     *         - "mail": The user's email address (String)
     *         - "is_premium": Whether the user has premium status (Boolean)
     *         - "user_picture": URL to the user's profile picture (String)
     *         Returns null if the request fails or the user is not authenticated
     *
     * @throws Exception If there's an error during the network request or data processing
     */
    suspend fun getUserData(context: Context): Map<String, Any>? {
        return withContext(Dispatchers.IO) {
            try {
                val sharedPreferences =
                    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token =
                    sharedPreferences.getString("auth_token", null) ?: return@withContext null

                val headers = mapOf("Authorization" to "Bearer $token")
                val response = getWithHeaders("user/profile", context, headers)

                if (response != null) {
                    val jsonResponse = JSONObject(response)

                    Log.d(
                        "UserData",
                        "Datos recibidos: $jsonResponse"
                    ) // Debug para ver qué devuelve la API

                    return@withContext mapOf(
                        "id" to jsonResponse.optInt("id", 0),
                        "nickname" to jsonResponse.optString("nickname", ""),
                        "mail" to jsonResponse.optString("mail", ""),
                        "is_premium" to jsonResponse.optBoolean("is_premium", false),
                        "user_picture" to jsonResponse.optString("user_picture", ""),
                        "daily_skips" to jsonResponse.optInt("daily_skips", 0),
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
    suspend fun getWithHeaders(
        endpoint: String,
        context: Context,
        headers: Map<String, String>
    ): String? {
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
    suspend fun putWithHeaders(
        endpoint: String,
        jsonBody: JSONObject,
        context: Context,
        headers: Map<String, String>
    ): String? {
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
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
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
                    Log.e(
                        "API",
                        "Error en la respuesta: código ${response.code}, mensaje: ${responseBody}"
                    )
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

    /**
     * Toggles the like status of a playlist for a specific user.
     *
     * This function sends a POST request to the server to like or unlike a playlist.
     * The actual toggle behavior (like/unlike) is handled by the server based on the
     * current state of the playlist-user relationship.
     *
     * @param playlistId The ID of the playlist to like or unlike
     * @param userId The ID of the user performing the action
     * @param isLiked Boolean indicating the desired like state (true for like, false for unlike)
     *                Note: This parameter is currently unused in the implementation but kept for API consistency
     * @return The server response message if successful, null otherwise
     *
     * @throws Exception If there's an error during the network request or response processing
     */
    suspend fun likeUnlikePlaylist(playlistId: String, userId: String, isLiked: Boolean): String? {
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

    /**
     * Retrieves a list of playlists that the user has liked.
     *
     * This function makes a GET request to the server to fetch all playlists that
     * have been liked by the specified user.
     *
     * @param userId The ID of the user whose liked playlists are being retrieved
     * @return A List of Playlist objects representing the user's liked playlists,
     *         or null if the request fails or the user ID is invalid
     *
     * @throws Exception If there's an error during the network request or when parsing the response
     */
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

    /**
     * Toggles the like status of a song for a specific user.
     *
     * This function sends a POST request to the server to like or unlike a song.
     * The actual toggle behavior (like/unlike) is handled by the server based on the
     * current state of the song-user relationship.
     *
     * @param songId The ID of the song to like or unlike
     * @param userId The ID of the user performing the action
     * @param isLiked Boolean indicating the desired like state (true for like, false for unlike)
     *                Note: This parameter is currently unused in the implementation but kept for consistency
     * @return The server response message if successful, null otherwise
     *
     * @throws Exception If there's an error during the network request or response processing
     */
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

    /**
     * Retrieves the list of songs that a user has marked as favorites.
     *
     * This function makes a GET request to the server to obtain all songs
     * that the specified user has liked. The response includes
     * complete data for each song.
     *
     * @param userId The ID of the user whose favorite songs are to be retrieved
     * @return A list of Song objects representing the user's favorite songs,
     *         or null if the request fails or if either ID is invalid
     *
     * @throws Exception If an error occurs during the network request or when processing the response
     */
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
                        val errorResponse =
                            connection.errorStream?.bufferedReader()?.use { it.readText() }
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
     * Checks if a song has been liked by a specific user.
     *
     * This function makes a GET request to the server to determine whether
     * the specified user has liked the given song.
     *
     * @param songId The ID of the song to check
     * @param userId The ID of the user whose like status is being checked
     * @return A boolean value: true if the user has liked the song, false otherwise
     *         Returns false if the request fails or if either ID is invalid
     *
     * @throws Exception If an error occurs during the network request or when processing the response
     */
    suspend fun checkIfSongIsLiked(songId: String?, userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (songId.isNullOrEmpty() || userId.isEmpty()) {
                    Log.e("checkIfSongIsLiked", "User ID or Song ID is invalid")
                    return@withContext false
                }

                // Use your actual base URL with query parameter for userId
                val url = URL("$BASE_URL/song_like/$songId/like?userId=$userId")

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
                        Log.d("checkIfSongIsLiked", "Response: $response")

                        val jsonObject = JSONObject(response)
                        return@withContext jsonObject.optBoolean("isLiked", false)
                    }

                    HttpURLConnection.HTTP_BAD_REQUEST -> {
                        Log.e("checkIfSongIsLiked", "Bad request - invalid parameters")
                        false
                    }

                    else -> {
                        Log.e("checkIfSongIsLiked", "Error response code: $responseCode")
                        val errorResponse =
                            connection.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e("checkIfSongIsLiked", "Error response: $errorResponse")
                        false
                    }
                }
            } catch (e: Exception) {
                Log.e("checkIfSongIsLiked", "Exception occurred", e)
                false
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

    /**
     * Retrieves all playlists created by a specific user.
     *
     * This function makes a GET request to the server to fetch all playlists
     * created by the specified user, excluding the default "Me Gusta" playlist.
     *
     * @param userId The ID of the user whose playlists are being retrieved
     * @return A list of Playlist objects representing the user's created playlists,
     *         or null if the request fails or the user ID is invalid
     *
     * @throws Exception If an error occurs during the network request or when processing the response
     */
    suspend fun getUserPlaylists(userId: String): List<Playlist>? {
        return withContext(Dispatchers.IO) {
            try {
                if (userId.isBlank()) {
                    Log.d("getUserPlaylists", "User ID is empty")
                    return@withContext null
                }

                val url = URL("$BASE_URL/playlists/users/$userId/playlists")
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
                        Log.d("getUserPlaylists", "Response: $response")

                        val jsonArray = JSONArray(response)
                        val userPlaylists = mutableListOf<Playlist>()

                        for (i in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(i)
                            userPlaylists.add(
                                Playlist(
                                    id = jsonObject.getString("id"),
                                    title = jsonObject.getString("name"),
                                    idAutor = jsonObject.getString("user_id"),
                                    idArtista = jsonObject.optString("artist_id", ""),
                                    description = jsonObject.optString("description", ""),
                                    esPublica = jsonObject.optString("type", ""),
                                    esAlbum = jsonObject.optString("typeP", ""),
                                    imageUrl = jsonObject.optString("front_page", "")
                                )
                            )
                        }
                        return@withContext userPlaylists
                    }

                    HttpURLConnection.HTTP_BAD_REQUEST -> {
                        Log.e("getUserPlaylists", "Bad request - invalid parameters")
                        null
                    }

                    else -> {
                        Log.e("getUserPlaylists", "Error response code: $responseCode")
                        val errorResponse =
                            connection.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e("getUserPlaylists", "Error response: $errorResponse")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("getUserPlaylists", "Exception occurred", e)
                null
            }
        }
    }

    /**
     * Obtiene la información detallada de una canción por su ID, incluyendo sus artistas.
     *
     * Esta función realiza una petición GET al endpoint `/player/details/:songId` para obtener
     * los detalles completos de una canción específica, incluyendo la lista de artistas asociados.
     *
     * @param songId El ID de la canción a consultar
     * @return Un objeto con la información de la canción y sus artistas, o null si hay error
     *
     * @throws IOException Si ocurre un error durante la comunicación con el servidor
     */
    suspend fun getSongDetails(songId: String): Map<String, Any>? {
        return withContext(Dispatchers.IO) {
            try {
                if (songId.isBlank()) {
                    Log.e("getSongDetails", "ID de canción inválido")
                    return@withContext null
                }

                val url = URL("$BASE_URL/player/details/$songId")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 10000 // 10 segundos
                    readTimeout = 10000 // 10 segundos
                }

                when (val responseCode = connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        Log.d("getSongDetails", "Respuesta: $response")

                        val jsonObject = JSONObject(response)

                        // Lista de artistas
                        val artistsArray = jsonObject.getJSONArray("artists")
                        val artists = (0 until artistsArray.length()).map { i ->
                            val artistObj = artistsArray.getJSONObject(i)
                            mapOf(
                                "id" to artistObj.getString("id"),
                                "name" to artistObj.getString("name")
                            )
                        }

                        // Datos completos de la canción
                        return@withContext mapOf(
                            "id" to jsonObject.getInt("id"),
                            "name" to jsonObject.getString("name"),
                            "duration" to jsonObject.getInt("duration"),
                            "lyrics" to jsonObject.optString("lyrics", ""),
                            "photo_video" to jsonObject.optString("photo_video", ""),
                            "url_mp3" to jsonObject.getString("url_mp3"),
                            "artists" to artists
                        )
                    }

                    HttpURLConnection.HTTP_NOT_FOUND -> {
                        Log.e("getSongDetails", "Canción no encontrada")
                        null
                    }

                    else -> {
                        Log.e("getSongDetails", "Error: código $responseCode")
                        val errorResponse =
                            connection.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e("getSongDetails", "Respuesta de error: $errorResponse")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("getSongDetails", "Excepción al obtener detalles de la canción", e)
                null
            }
        }
    }

    /**
     * Gestiona la adición o eliminación de una canción en una playlist específica.
     *
     * Esta función realiza una petición POST al endpoint `/playlists/:playlistId/:operation/handleSong`
     * para añadir o eliminar una canción de una playlist según la operación especificada.
     *
     * @param playlistId El ID de la playlist a modificar
     * @param songId El ID de la canción a añadir o eliminar
     * @param operation La operación a realizar ("add" para añadir, "remove" para eliminar)
     * @return La respuesta del servidor como String, o null si hay error
     *
     * @throws IOException Si ocurre un error durante la comunicación con el servidor
     */
    suspend fun handleSongToPlaylist(
        playlistId: String,
        songId: String,
        operation: Boolean
    ): Map<String, Any>? {
        return withContext(Dispatchers.IO) {
            try {
                // Validar parámetros
                if (playlistId.isBlank() || songId.isBlank()) {
                    Log.e("handleSongToPlaylist", "ID de playlist o canción inválido")
                    return@withContext null
                }

                val operation = if (operation) "add" else "remove"
                if (operation != "add" && operation != "remove") {
                    Log.e("handleSongToPlaylist", "Operación inválida. Debe ser 'add' o 'remove'")
                    return@withContext null
                }

                val url = URL("$BASE_URL/playlists/$playlistId/$operation/handleSong")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 10000 // 10 segundos
                    readTimeout = 10000 // 10 segundos
                    doOutput = true
                }

                // Crear el cuerpo JSON
                val jsonBody = JSONObject().apply {
                    put("songId", songId)
                }

                // Enviar el cuerpo de la solicitud
                connection.outputStream.use { os ->
                    os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                }

                when (val responseCode = connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        Log.d("handleSongToPlaylist", "Respuesta: $response")

                        val jsonObject = JSONObject(response)
                        return@withContext mapOf(
                            "message" to jsonObject.getString("message"),
                            "operation" to jsonObject.getString("operation"),
                            "success" to true
                        )
                    }

                    HttpURLConnection.HTTP_BAD_REQUEST -> {
                        val errorResponse =
                            connection.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e("handleSongToPlaylist", "Error en la solicitud: $errorResponse")

                        val jsonObject = JSONObject(errorResponse ?: "{}")
                        return@withContext mapOf(
                            "error" to jsonObject.optString("error", "Error en la solicitud"),
                            "success" to false
                        )
                    }

                    HttpURLConnection.HTTP_NOT_FOUND -> {
                        val errorResponse =
                            connection.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e("handleSongToPlaylist", "Recurso no encontrado: $errorResponse")

                        val jsonObject = JSONObject(errorResponse ?: "{}")
                        return@withContext mapOf(
                            "error" to jsonObject.optString(
                                "error",
                                "Canción no encontrada en la playlist"
                            ),
                            "success" to false
                        )
                    }

                    else -> {
                        val errorResponse =
                            connection.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e(
                            "handleSongToPlaylist",
                            "Error: código $responseCode, respuesta: $errorResponse"
                        )
                        return@withContext mapOf(
                            "error" to "Error del servidor",
                            "success" to false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("handleSongToPlaylist", "Excepción al gestionar canción en playlist", e)
                return@withContext mapOf(
                    "error" to (e.message ?: "Error desconocido"),
                    "success" to false
                )
            }
        }
    }


    /**
     * Obtiene todas las playlists que contienen una canción específica.
     *
     * @param songId El ID de la canción para buscar en las playlists
     * @return Una lista de objetos Playlist que contienen la canción especificada,
     *         o null si hay un error de validación o en la solicitud
     */
    suspend fun getPlaylistsBySongId(songId: String): List<Playlist>? {
        return withContext(Dispatchers.IO) {
            try {
                // Validar parámetros
                if (songId.isBlank()) {
                    Log.e("getPlaylistsBySongId", "ID de canción inválido")
                    return@withContext null
                }

                val url = URL("$BASE_URL/playlists/$songId/songPlaylists")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 10000 // 10 segundos
                    readTimeout = 10000 // 10 segundos
                }

                when (val responseCode = connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        Log.d("getPlaylistsBySongId", "Respuesta: $response")

                        val jsonObject = JSONObject(response)
                        val playlistsArray = jsonObject.getJSONArray("playlists")
                        val songPlaylists = mutableListOf<Playlist>()

                        for (i in 0 until playlistsArray.length()) {
                            val jsonObject = playlistsArray.getJSONObject(i)
                            songPlaylists.add(
                                Playlist(
                                    id = jsonObject.getString("id"),
                                    title = jsonObject.getString("name"),
                                    idAutor = jsonObject.getString("user_id"),
                                    idArtista = jsonObject.optString("artist_id", ""),
                                    description = jsonObject.optString("description", ""),
                                    esPublica = jsonObject.optString("type", ""),
                                    esAlbum = jsonObject.optString("typeP", ""),
                                    imageUrl = jsonObject.optString("front_page", "")
                                )
                            )
                        }
                        return@withContext songPlaylists
                    }

                    HttpURLConnection.HTTP_BAD_REQUEST -> {
                        Log.e("getPlaylistsBySongId", "Solicitud incorrecta - parámetros inválidos")
                        null
                    }

                    HttpURLConnection.HTTP_NOT_FOUND -> {
                        Log.e("getPlaylistsBySongId", "No se encontraron playlists con esta canción")
                        // Devolver una lista vacía en lugar de null cuando no hay playlists
                        emptyList()
                    }

                    else -> {
                        Log.e("getPlaylistsBySongId", "Código de respuesta de error: $responseCode")
                        val errorResponse =
                            connection.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e("getPlaylistsBySongId", "Respuesta de error: $errorResponse")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("getPlaylistsBySongId", "Excepción al obtener playlists por ID de canción", e)
                null
            }
        }
    }

    /**
     * Verifica si un usuario es el propietario de una playlist específica.
     *
     * @param playlistId El ID de la playlist a verificar
     * @param userId El ID del usuario a verificar como propietario
     * @return true si el usuario es propietario de la playlist, false si no lo es o null si hay un error
     */
    suspend fun isPlaylistOwner(playlistId: String, userId: String): Boolean? {
        return withContext(Dispatchers.IO) {
            try {
                // Validar parámetros
                if (playlistId.isBlank() || userId.isBlank()) {
                    Log.e("isPlaylistOwner", "ID de playlist o usuario inválido")
                    return@withContext null
                }

                val url = URL("$BASE_URL/playlists/$playlistId/isOwner/$userId")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 10000 // 10 segundos
                    readTimeout = 10000 // 10 segundos
                }

                when (val responseCode = connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        Log.d("isPlaylistOwner", "Respuesta: $response")

                        val jsonObject = JSONObject(response)
                        return@withContext jsonObject.getBoolean("isOwner")
                    }

                    HttpURLConnection.HTTP_BAD_REQUEST -> {
                        Log.e("isPlaylistOwner", "Solicitud incorrecta - parámetros inválidos")
                        null
                    }

                    HttpURLConnection.HTTP_NOT_FOUND -> {
                        Log.e("isPlaylistOwner", "La playlist especificada no existe")
                        null
                    }

                    else -> {
                        Log.e("isPlaylistOwner", "Código de respuesta de error: $responseCode")
                        val errorResponse =
                            connection.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e("isPlaylistOwner", "Respuesta de error: $errorResponse")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("isPlaylistOwner", "Excepción al verificar propiedad de playlist", e)
                null
            }
        }
    }

    /**
     * Cambia el tipo de una playlist entre 'public' y 'private'.
     *
     * @param playlistId El ID de la playlist a modificar
     * @param currentPlaylist La playlist actual para obtener su tipo
     * @return El nuevo tipo de la playlist ('public' o 'private'), o null si hay un error
     */
    suspend fun togglePlaylistType(playlistId: String, playlistInfo: Playlist): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Validar parámetro
                if (playlistId.isBlank()) {
                    Log.e("togglePlaylistType", "ID de playlist inválido")
                    return@withContext null
                }

                // Determinamos el nuevo tipo
                val currentType = playlistInfo.esPublica
                val newType = if (currentType == "private") "public" else "private"

                val url = URL("$BASE_URL/playlists/$playlistId")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "PUT"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 10000 // 10 segundos
                    readTimeout = 10000 // 10 segundos
                    doOutput = true
                }

                // Creamos el cuerpo de la petición, manteniendo los valores actuales excepto el tipo
                val requestBody = JSONObject().apply {
                    put("name", playlistInfo.title)
                    put("description", playlistInfo.description)
                    put("type", newType)
                    put("front_page", playlistInfo.imageUrl)
                }

                // Enviamos los datos en el cuerpo de la petición
                val outputStream = DataOutputStream(connection.outputStream)
                outputStream.writeBytes(requestBody.toString())
                outputStream.flush()
                outputStream.close()

                when (val responseCode = connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        Log.d("togglePlaylistType", "Respuesta: $response")

                        val jsonObject = JSONObject(response)

                        // Actualizamos la información de la playlist en memoria
                        playlistInfo.esPublica = newType

                        return@withContext newType
                    }

                    HttpURLConnection.HTTP_NOT_FOUND -> {
                        Log.e("togglePlaylistType", "La playlist especificada no existe")
                        null
                    }

                    else -> {
                        Log.e("togglePlaylistType", "Código de respuesta de error: $responseCode")
                        val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e("togglePlaylistType", "Respuesta de error: $errorResponse")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("togglePlaylistType", "Excepción al cambiar el tipo de la playlist", e)
                null
            }
        }
    }

    /**
     * Actualiza los datos del usuario autenticado.
     * @param currentPassword Contraseña actual (obligatorio)
     * @param nickname Nuevo nickname (opcional)
     * @param email Nuevo correo electrónico (opcional)
     * @param password Nueva contraseña (opcional)
     * @return Pair<Código de respuesta, Mensaje del servidor>
     */
    suspend fun updateUserProfile(
        currentPassword: String,
        nickname: String?,
        email: String?,
        password: String?,
        context: Context
    ): Pair<Int, String?> = withContext(Dispatchers.IO) {
        try {
            val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val token = sharedPreferences.getString("auth_token", null)
                ?: return@withContext Pair(401, "No autenticado")

            // Validar al menos un campo opcional
            if (nickname.isNullOrBlank() && email.isNullOrBlank() && password.isNullOrBlank()) {
                return@withContext Pair(422, "Debes proporcionar al menos un campo para actualizar")
            }

            val jsonBody = JSONObject().apply {
                put("currentPassword", currentPassword)
                nickname?.takeIf { it.isNotBlank() }?.let { put("nickname", it) }
                email?.takeIf { it.isNotBlank() }?.let { put("mail", it) }
                password?.takeIf { it.isNotBlank() }?.let { put("password", it) }
            }

            val client = OkHttpClient()
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = jsonBody.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL/user/update")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $token")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.d("UpdateProfile", "Código: ${response.code}, Respuesta: $responseBody")
                return@withContext Pair(response.code, responseBody)
            }
        } catch (e: Exception) {
            Log.e("UpdateProfile", "Error: ${e.message}")
            Pair(500, "Error de conexión")
        }
    }


    suspend fun forgotPassword(email: String, context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    put("mail", email)
                }

                val response = postWithHeaders(
                    endpoint = "user/forgot-password",
                    jsonBody = jsonBody,
                    context = context,
                    responseHeaders = mutableMapOf()
                )

                response?.let {
                    val jsonResponse = JSONObject(it)
                    val status = jsonResponse.optInt("status", 200)
                    status in 200..299
                } ?: false

            } catch (e: Exception) {
                Log.e("ForgotPassword", "Error: ${e.message}")
                false
            }
        }
    }

    suspend fun resetPassword(token: String, newPassword: String, context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    put("token", token)
                    put("newPassword", newPassword)
                }

                val response = postWithHeaders(
                    endpoint = "user/reset-password",
                    jsonBody = jsonBody,
                    context = context,
                    responseHeaders = mutableMapOf()
                )

                response?.let {
                    val jsonResponse = JSONObject(it)
                    val status = jsonResponse.optInt("status", 200)
                    status in 200..299
                } ?: false

            } catch (e: Exception) {
                Log.e("ResetPassword", "Error: ${e.message}")
                false
            }
        }
    }

    /**
     * Actualiza los datos de una playlist existente
     * @param id Identificador único de la playlist a actualizar
     * @param name Nuevo nombre de la playlist (opcional)
     * @param description Nueva descripción (opcional)
     * @param type Nuevo tipo de contenido (opcional)
     * @param frontPage Nueva imagen de portada (opcional)
     * @return Pair<Código de respuesta HTTP, Mensaje de respuesta>
     */
    suspend fun updatePlaylist(
        id: Int,
        name: String? = null,
        description: String? = null,
        type: String? = null,
        frontPage: String? = null,
        context: Context
    ): Pair<Int, String?> = withContext(Dispatchers.IO) {
        try {
            if (id <= 0) {
                Log.e("UpdatePlaylist", "ID inválido: $id")
                return@withContext Pair(400, "ID inválido")
            }
            val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val token = sharedPreferences.getString("auth_token", null)
                ?: return@withContext Pair(401, "No autenticado")

            val jsonBody = JSONObject().apply {
                name?.takeIf { it.isNotBlank() }?.let { put("name", it) }
                description?.takeIf { it.isNotBlank() }?.let { put("description", it) }
                type?.takeIf { it.isNotBlank() }?.let { put("type", it) }
                frontPage?.takeIf { it.isNotBlank() }?.let { put("front_page", it) }
            }

            val client = OkHttpClient()
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = jsonBody.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("${BASE_URL}/playlists/$id")
                .put(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $token")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                return@withContext Pair(response.code, responseBody)
            }
        } catch (e: Exception) {
            Log.e("UpdatePlaylist", "Error: ${e.message}")
            Pair(500, "Error de conexión")
        }
    }

    /**
     * Obtiene la valoración promedio de una playlist específica desde el backend.
     *
     * @param playlistId El ID de la playlist de la que se quiere obtener la valoración.
     * @return El valor promedio de la valoración (entre 0.0 y 5.0), o `null` si ocurre un error.
     *
     * @throws Exception Si hay un problema al realizar la solicitud al servidor.
     */
    suspend fun getPlaylistAverageRating(playlistId: String): Double? {
        return try {
            val response = get("ratingPlaylist/$playlistId/rating")
            response?.let {
                val json = JSONObject(it)
                json.optDouble("average", 0.0)
            }
        } catch (e: Exception) {
            println("Error al obtener el rating promedio: ${e.message}")
            null
        }
    }
    
    /**
     * Envía una valoración de un usuario para una playlist específica.
     *
     * @param playlistId El ID de la playlist que se desea valorar.
     * @param userId El ID del usuario que está realizando la valoración.
     * @param rating La puntuación dada por el usuario (normalmente de 1 a 5).
     * @return `true` si la valoración se envió correctamente, `false` si hubo un error.
     *
     * @throws Exception Si ocurre un problema al comunicarse con el servidor.
     */
    suspend fun ratePlaylist(playlistId: String, userId: String, rating: Int): Boolean {
        return try {
            val jsonBody = JSONObject().apply {
                put("user_id", userId) // Aquí añadimos el user_id
                put("rating", rating)
            }
            val response = post("ratingPlaylist/$playlistId/rate", jsonBody)
            response != null
        } catch (e: Exception) {
            println("Error al valorar la playlist: ${e.message}")
            false
        }
    }
          
    /*
     * Función para consumir el endpoint que resta un skip diario a un usuario.
     *
     * @param userId ID del usuario al que se le desea restar un skip.
     * @return JSONObject con la respuesta del servidor o `null` en caso de error.
     */
    suspend fun skipsLessApi(userId: String): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                val request = Request.Builder()
                    .url("$BASE_URL/user/use-daily-skip/$userId")
                    .post("".toRequestBody("application/json".toMediaTypeOrNull()))
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()

                    if (!response.isSuccessful) {
                        Log.e("API", "Error en la respuesta: código ${response.code}, cuerpo: $responseBody")
                        if (response.code == 400 && responseBody != null) {
                            // Return the error response so we can handle it properly
                            return@withContext JSONObject(responseBody)
                        }
                        null
                    } else {
                        Log.d("API", "Respuesta skip: $responseBody")
                        if (responseBody != null) {
                            JSONObject(responseBody)
                        } else {
                            null
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ApiClient", "Error en skipsLessApi: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Función para registrar una visita a una playlist por parte de un usuario.
     *
     * @param playlistId ID de la playlist visitada.
     * @param userId ID del usuario que visita la playlist.
     * @return JSONObject con la respuesta del servidor o `null` en caso de error.
     */
    suspend fun recordPlaylistVisit(playlistId: String, userId: String): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                val jsonBody = JSONObject().apply {
                    put("userId", userId)
                }

                val requestBody = jsonBody.toString()
                    .toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("$BASE_URL/playlists/$playlistId/visit")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()

                    if (!response.isSuccessful) {
                        Log.e("API", "Error al registrar visita: código ${response.code}, cuerpo: $responseBody")
                        if (response.code == 400 && responseBody != null) {
                            // Devolver la respuesta de error para manejarla adecuadamente
                            return@withContext JSONObject(responseBody)
                        }
                        null
                    } else {
                        Log.d("API", "Visita registrada: $responseBody")
                        if (responseBody != null) {
                            JSONObject(responseBody)
                        } else {
                            null
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ApiClient", "Error en recordPlaylistVisit: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Función para actualizar la información de un usuario.
     *
     * @param userId ID del usuario a actualizar.
     * @param nickname Nuevo nombre de usuario (opcional).
     * @param profileImage Imagen de perfil en formato base64 (opcional).
     * @return JSONObject con la respuesta del servidor o `null` en caso de error.
     */
    suspend fun updateUserProfile(userId: String, nickname: String? = null, profileImage: String? = null): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                val jsonBody = JSONObject().apply {
                    nickname?.let { put("nickname", it) }
                    profileImage?.let { put("profileImage", it) }
                }

                val requestBody = jsonBody.toString()
                    .toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("$BASE_URL/user/users/$userId")
                    .post(requestBody)  // Usando POST para actualizar recurso
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()

                    if (!response.isSuccessful) {
                        Log.e("API", "Error al actualizar usuario: código ${response.code}, cuerpo: $responseBody")
                        if (responseBody != null) {
                            // Devolver la respuesta de error para manejarla adecuadamente
                            return@withContext JSONObject(responseBody)
                        }
                        null
                    } else {
                        Log.d("API", "Usuario actualizado: $responseBody")
                        if (responseBody != null) {
                            JSONObject(responseBody)
                        } else {
                            null
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ApiClient", "Error en updateUserProfile: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }


    /**
     * Convierte una imagen identificada por un URI a una cadena en formato Base64.
     *
     * @param context El contexto de la aplicación necesario para acceder al ContentResolver
     * @param uri El URI de la imagen que se va a convertir
     * @return Una cadena en formato data URL con la imagen codificada en Base64, o null si ocurre un error
     */
    suspend fun uriToBase64(context: Context, uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()

                "data:image/jpeg;base64,${android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)}"
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Función para actualizar la imagen de portada de una playlist.
     *
     * @param playlistId ID de la playlist a actualizar.
     * @param frontPageImage Imagen de portada en formato base64 (debe incluir el prefijo "data:image/...").
     * @return JSONObject con la respuesta del servidor o `null` en caso de error.
     */
    suspend fun updatePlaylistImage(playlistId: Int, frontPageImage: String): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                // Crear el objeto JSON con solo la información de la imagen
                val jsonBody = JSONObject().apply {
                    put("front_page", frontPageImage)
                }

                val requestBody = jsonBody.toString()
                    .toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("$BASE_URL/playlists/$playlistId")
                    .put(requestBody)  // Usando PUT para actualizar un recurso existente
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()

                    if (!response.isSuccessful) {
                        Log.e("API", "Error al actualizar imagen de playlist: código ${response.code}, cuerpo: $responseBody")
                        if (responseBody != null) {
                            // Devolver la respuesta de error para manejarla adecuadamente
                            return@withContext JSONObject(responseBody)
                        }
                        null
                    } else {
                        Log.d("API", "Imagen de playlist actualizada: $responseBody")
                        if (responseBody != null) {
                            JSONObject(responseBody)
                        } else {
                            null
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ApiClient", "Error en updatePlaylistImage: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Función para obtener playlists recomendadas basadas en los estilos favoritos del usuario.
     *
     * @param context Contexto de la aplicación para acceder a SharedPreferences.
     * @return JSONObject con las playlists recomendadas o `null` en caso de error.
     */
    suspend fun getRecommendedPlaylistsForUser(context: Context): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                // Obtener el token desde SharedPreferences
                val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("auth_token", null)

                if (token.isNullOrEmpty()) {
                    Log.e("API", "Token no disponible para obtener playlists recomendadas")
                    return@withContext null
                }

                val client = OkHttpClient()

                // No necesitamos enviar datos en el cuerpo ya que la API extrae toda la información
                // necesaria del token JWT (ID de usuario y estilos favoritos)
                val requestBody = "{}".toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("$BASE_URL/user/recommended-playlists")
                    .get()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                Log.d("API", "Solicitando playlists recomendadas para el usuario")

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()

                    if (!response.isSuccessful) {
                        Log.e("API", "Error al obtener playlists recomendadas: código ${response.code}, cuerpo: $responseBody")
                        if (responseBody != null) {
                            // Devolver la respuesta de error para manejarla adecuadamente
                            return@withContext JSONObject(responseBody)
                        }
                        null
                    } else {
                        Log.d("API", "Playlists recomendadas obtenidas: $responseBody")
                        if (responseBody != null) {
                            JSONObject(responseBody)
                        } else {
                            null
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ApiClient", "Error en getRecommendedPlaylistsForUser: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Función que procesa el JSON de respuesta de playlists recomendadas.
     *
     * @param response El objeto JSONObject devuelto por getRecommendedPlaylistsForUser.
     * @return Lista de objetos Playlist o lista vacía si hay error.
     */
    fun processRecommendedPlaylists(response: JSONObject?): List<Playlist> {
        if (response == null) return emptyList()

        val recommendedPlaylists = mutableListOf<Playlist>()

        try {
            // Verificamos si existe la clave "recommendedPlaylists" en el JSON
            if (response.has("recommendedPlaylists")) {
                val playlistsArray = response.getJSONArray("recommendedPlaylists")

                for (i in 0 until playlistsArray.length()) {
                    val playlistObj = playlistsArray.getJSONObject(i)

                    // Extracción segura de campos con valores por defecto
                    val playlist = Playlist(
                        id = playlistObj.optString("id", ""),
                        title = playlistObj.optString("name", "Sin título"),
                        idAutor = playlistObj.optString("user_id", ""),
                        idArtista = playlistObj.optString("artist_id", ""),
                        description = playlistObj.optString("description", ""),
                        esPublica = playlistObj.optString("type", "public"),
                        esAlbum = playlistObj.optString("typeP", "playlist"),
                        imageUrl = playlistObj.optString("front_page", "")
                    )

                    // Solo añadimos playlists que tienen id válido
                    if (playlist.id.isNotEmpty()) {
                        recommendedPlaylists.add(playlist)
                        Log.d("Recommendations", "Agregada playlist: ${playlist.title} con ID: ${playlist.id}")
                    }
                }
            } else {
                Log.w("Recommendations", "El JSON no contiene la clave 'recommendedPlaylists'")
            }

        } catch (e: Exception) {
            Log.e("Recommendations", "Error procesando playlists recomendadas: ${e.message}", e)
        }

        Log.d("Recommendations", "Total de playlists procesadas: ${recommendedPlaylists.size}")
        return recommendedPlaylists
    }

    /**
     * Busca usuarios que pueden ser añadidos como amigos.
     */
    suspend fun searchNewFriends(context: Context): JSONArray? {
        return withContext(Dispatchers.IO) {
            try {
                val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("auth_token", null)
                
                if (token.isNullOrEmpty()) {
                    Log.e("API", "Token no disponible para buscar amigos")
                    return@withContext null
                }
                
                // La URL correcta según el backend
                val url = URL("$BASE_URL/social/getNewFriends")
                
                Log.d("API", "URL de búsqueda: ${url.toString()}")
                
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST" // Cambiado a POST
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    doOutput = true // Necesario para POST
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                
//                // Escribir el cuerpo JSON con el término de búsqueda
//                connection.outputStream.use { os ->
//                    os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
//                    os.flush()
//                }
                
                val responseCode = connection.responseCode
                Log.d("API", "Código de respuesta searchNewFriends: $responseCode")
                
                if (responseCode in 200..299) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("API", "Respuesta searchNewFriends: $response")
                    
                    val jsonObject = JSONObject(response)
                    if (jsonObject.has("users")) {
                        return@withContext jsonObject.getJSONArray("users")
                    } else {
                        Log.d("API", "No se encontraron resultados en la búsqueda")
                        return@withContext JSONArray()
                    }
                } else {
                    val errorStream = connection.errorStream
                    val errorBody = errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
                    Log.e("API", "Error en searchNewFriends: $errorBody")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("API", "Excepción en searchNewFriends: ${e.message}", e)
                return@withContext null
            }
        }
    }

    /**
     * Obtiene las solicitudes de amistad recibidas por el usuario actual.
     */
    suspend fun getReceivedFriendRequests(context: Context): JSONArray? {
        return withContext(Dispatchers.IO) {
            try {
                val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("auth_token", null)
                
                if (token.isNullOrEmpty()) {
                    Log.e("API", "Token no disponible para obtener solicitudes recibidas")
                    return@withContext null
                }
                
                // La URL correcta según el backend
                val url = URL("$BASE_URL/social/getReceivedFriendRequests")
                
                Log.d("API", "URL de solicitudes recibidas: ${url.toString()}")
                
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST" // Cambiado a POST
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    doOutput = true // Necesario para POST
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                
                // Enviar un objeto JSON vacío como cuerpo
                val emptyJsonBody = JSONObject()
                connection.outputStream.use { os ->
                    os.write(emptyJsonBody.toString().toByteArray(Charsets.UTF_8))
                    os.flush()
                }
                
                val responseCode = connection.responseCode
                Log.d("API", "Código de respuesta getReceivedFriendRequests: $responseCode")
                
                if (responseCode in 200..299) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("API", "Respuesta getReceivedFriendRequests: $response")
                    
                    val jsonObject = JSONObject(response)
                    if (jsonObject.has("receivedRequests")) {
                        return@withContext jsonObject.getJSONArray("receivedRequests")
                    } else {
                        Log.d("API", "No se encontraron solicitudes recibidas")
                        return@withContext JSONArray()
                    }
                } else {
                    val errorStream = connection.errorStream
                    val errorBody = errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
                    Log.e("API", "Error en getReceivedFriendRequests: $errorBody")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("API", "Excepción en getReceivedFriendRequests: ${e.message}", e)
                return@withContext null
            }
        }
    }

    /**
     * Obtiene la lista de amigos del usuario actual.
     */
    suspend fun getFriendsList(context: Context): JSONArray? {
        return withContext(Dispatchers.IO) {
            try {
                val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("auth_token", null)
                
                if (token.isNullOrEmpty()) {
                    Log.e("API", "Token no disponible para obtener lista de amigos")
                    return@withContext null
                }
                
                // La URL correcta según el backend
                val url = URL("$BASE_URL/social/getFriendsList")
                
                Log.d("API", "URL de lista de amigos: ${url.toString()}")
                
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST" // Cambiado a POST
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    doOutput = true // Necesario para POST
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                
                // Enviar un objeto JSON vacío como cuerpo
                val emptyJsonBody = JSONObject()
                connection.outputStream.use { os ->
                    os.write(emptyJsonBody.toString().toByteArray(Charsets.UTF_8))
                    os.flush()
                }
                
                val responseCode = connection.responseCode
                Log.d("API", "Código de respuesta getFriendsList: $responseCode")
                
                if (responseCode in 200..299) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("API", "Respuesta getFriendsList: $response")
                    
                    val jsonObject = JSONObject(response)
                    if (jsonObject.has("friends")) {
                        return@withContext jsonObject.getJSONArray("friends")
                    } else {
                        Log.d("API", "No se encontraron amigos")
                        return@withContext JSONArray()
                    }
                } else {
                    val errorStream = connection.errorStream
                    val errorBody = errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
                    Log.e("API", "Error en getFriendsList: $errorBody")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("API", "Excepción en getFriendsList: ${e.message}", e)
                return@withContext null
            }
        }
    }

    /**
     * Envía una solicitud de amistad a otro usuario.
     */
    suspend fun sendFriendRequest(friendId: String, context: Context): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("auth_token", null)
                
                if (token.isNullOrEmpty()) {
                    Log.e("API", "Token no disponible para enviar solicitud de amistad")
                    return@withContext null
                }
                
                // Según el código del backend, la ruta correcta es "/social/send"
                val url = URL("$BASE_URL/social/send")
                
                Log.d("API", "URL de envío de solicitud: ${url.toString()}")
                
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                
                // Según el código del backend, se espera "user2_id" en el cuerpo
                val jsonBody = JSONObject().apply {
                    put("user2_id", friendId)
                }
                
                // Escribir el cuerpo JSON
                connection.outputStream.use { os ->
                    os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                    os.flush()
                }
                
                val responseCode = connection.responseCode
                Log.d("API", "Código de respuesta sendFriendRequest: $responseCode")
                
                if (responseCode in 200..299) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("API", "Respuesta sendFriendRequest: $response")
                    return@withContext JSONObject(response)
                } else {
                    val errorStream = connection.errorStream
                    val errorBody = errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
                    Log.e("API", "Error en sendFriendRequest: $errorBody")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("API", "Excepción en sendFriendRequest: ${e.message}", e)
                return@withContext null
            }
        }
    }

    /**
     * Acepta una solicitud de amistad recibida.
     * 
     * @param user1Id ID del usuario que envió la solicitud
     * @param context Contexto para obtener el token de autenticación
     * @return Objeto JSON con la respuesta o null si hay error
     */
    suspend fun acceptFriendRequest(user1Id: String, context: Context): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("auth_token", null)
                
                if (token.isNullOrEmpty()) {
                    Log.e("API", "Token no disponible para aceptar solicitud de amistad")
                    return@withContext null
                }
                
                // La URL correcta según el backend
                val url = URL("$BASE_URL/social/accept")
                
                Log.d("API", "URL para aceptar solicitud: ${url.toString()}")
                
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                
                // Según el controlador del backend, se espera "user1_id" en el cuerpo
                val jsonBody = JSONObject().apply {
                    put("user1_id", user1Id)
                }
                
                // Escribir el cuerpo JSON
                connection.outputStream.use { os ->
                    os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                    os.flush()
                }
                
                val responseCode = connection.responseCode
                Log.d("API", "Código de respuesta acceptFriendRequest: $responseCode")
                
                if (responseCode in 200..299) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("API", "Respuesta acceptFriendRequest: $response")
                    return@withContext JSONObject(response)
                } else {
                    val errorStream = connection.errorStream
                    val errorBody = errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
                    Log.e("API", "Error en acceptFriendRequest: $errorBody")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("API", "Excepción en acceptFriendRequest: ${e.message}", e)
                return@withContext null
            }
        }
    }

    /**
     * Rechaza una solicitud de amistad pendiente.
     * 
     * @param friendId ID del usuario cuya solicitud se rechaza
     * @param context Contexto para obtener el token de autenticación
     * @return Objeto JSON con la respuesta o null si hay error
     */
    suspend fun rejectFriendRequest(friendId: String, context: Context): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("auth_token", null)
                
                if (token.isNullOrEmpty()) {
                    Log.e("API", "Token no disponible para rechazar solicitud de amistad")
                    return@withContext null
                }
                
                // La URL correcta según el backend
                val url = URL("$BASE_URL/social/reject")
                
                Log.d("API", "URL para rechazar solicitud: ${url.toString()}")
                
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                
                // Según el controlador del backend, se espera "friendId" en el cuerpo
                val jsonBody = JSONObject().apply {
                    put("friendId", friendId)
                }
                
                // Escribir el cuerpo JSON
                connection.outputStream.use { os ->
                    os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                    os.flush()
                }
                
                val responseCode = connection.responseCode
                Log.d("API", "Código de respuesta rejectFriendRequest: $responseCode")
                
                if (responseCode in 200..299) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("API", "Respuesta rejectFriendRequest: $response")
                    return@withContext JSONObject(response)
                } else {
                    val errorStream = connection.errorStream
                    val errorBody = errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
                    Log.e("API", "Error en rejectFriendRequest: $errorBody")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("API", "Excepción en rejectFriendRequest: ${e.message}", e)
                return@withContext null
            }
        }
    }

    /**
     * Elimina a un amigo de la lista de amigos.
     * 
     * @param friendId ID del usuario que se eliminará como amigo
     * @param context Contexto para obtener el token de autenticación
     * @return Objeto JSON con la respuesta o null si hay error
     */
    suspend fun unfollowFriend(friendId: String, context: Context): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("auth_token", null)
                
                if (token.isNullOrEmpty()) {
                    Log.e("API", "Token no disponible para eliminar amigo")
                    return@withContext null
                }
                
                // La URL correcta según el backend
                val url = URL("$BASE_URL/social/unfollow")
                
                Log.d("API", "URL para eliminar amigo: ${url.toString()}")
                
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                
                // Según el controlador del backend, se espera "friendId" en el cuerpo
                val jsonBody = JSONObject().apply {
                    put("friendId", friendId)
                }
                
                // Escribir el cuerpo JSON
                connection.outputStream.use { os ->
                    os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                    os.flush()
                }
                
                val responseCode = connection.responseCode
                Log.d("API", "Código de respuesta unfollowFriend: $responseCode")
                
                if (responseCode in 200..299) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("API", "Respuesta unfollowFriend: $response")
                    return@withContext JSONObject(response)
                } else {
                    val errorStream = connection.errorStream
                    val errorBody = errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
                    Log.e("API", "Error en unfollowFriend: $errorBody")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("API", "Excepción en unfollowFriend: ${e.message}", e)
                return@withContext null
            }
        }
    }

    /**
     * Obtiene las solicitudes de amistad enviadas por el usuario actual.
     */
    suspend fun getSentFriendRequests(context: Context): JSONArray? {
        return withContext(Dispatchers.IO) {
            try {
                val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("auth_token", null)
                
                if (token.isNullOrEmpty()) {
                    Log.e("API", "Token no disponible para obtener solicitudes enviadas")
                    return@withContext null
                }
                
                // La URL correcta según el backend
                val url = URL("$BASE_URL/social/getSentFriendRequests")
                
                Log.d("API", "URL de solicitudes enviadas: ${url.toString()}")
                
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST" // El backend espera POST
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    doOutput = true // Necesario para POST
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                
                // Enviar un objeto JSON vacío como cuerpo
                val emptyJsonBody = JSONObject()
                connection.outputStream.use { os ->
                    os.write(emptyJsonBody.toString().toByteArray(Charsets.UTF_8))
                    os.flush()
                }
                
                val responseCode = connection.responseCode
                Log.d("API", "Código de respuesta getSentFriendRequests: $responseCode")
                
                if (responseCode in 200..299) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("API", "Respuesta getSentFriendRequests: $response")
                    
                    val jsonObject = JSONObject(response)
                    if (jsonObject.has("sentRequests")) {
                        return@withContext jsonObject.getJSONArray("sentRequests")
                    } else {
                        Log.d("API", "No se encontraron solicitudes enviadas")
                        return@withContext JSONArray()
                    }
                } else {
                    val errorStream = connection.errorStream
                    val errorBody = errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
                    Log.e("API", "Error en getSentFriendRequests: $errorBody")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("API", "Excepción en getSentFriendRequests: ${e.message}", e)
                return@withContext null
            }
        }
    }

    /**
     * Obtiene el historial de mensajes entre el usuario actual y un amigo.
     * 
     * @param friendId ID del amigo con quien se tiene la conversación
     * @param context Contexto para obtener el token de autenticación
     * @return JSONObject con los mensajes de la conversación o null si hay un error
     */
    suspend fun getChatConversation(friendId: String, context: Context): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("auth_token", null)
                
                if (token.isNullOrEmpty()) {
                    Log.e("API", "Token no disponible para obtener conversación de chat")
                    return@withContext null
                }
                
                // URL correcta para obtener la conversación
                val url = URL("$BASE_URL/chat/conversation/$friendId")
                
                Log.d("API", "URL para obtener conversación de chat: $url")
                
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                
                val responseCode = connection.responseCode
                Log.d("API", "Código de respuesta getChatConversation: $responseCode")
                
                if (responseCode in 200..299) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("API", "Respuesta getChatConversation: $response")
                    return@withContext JSONObject(response)
                } else {
                    val errorStream = connection.errorStream
                    val errorBody = errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
                    Log.e("API", "Error en getChatConversation: $errorBody")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("API", "Excepción en getChatConversation: ${e.message}", e)
                return@withContext null
            }
        }
    }

    /**
     * Envía un mensaje a un amigo.
     * 
     * @param friendId ID del amigo a quien se envía el mensaje
     * @param message Texto del mensaje a enviar
     * @param context Contexto para obtener el token de autenticación
     * @return JSONObject con la respuesta del servidor o null si hay un error
     */
    suspend fun sendChatMessage(friendId: String, message: String, context: Context): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("auth_token", null)
                
                if (token.isNullOrEmpty()) {
                    Log.e("API", "Token no disponible para enviar mensaje de chat")
                    return@withContext null
                }
                
                // URL correcta para enviar un mensaje
                val url = URL("$BASE_URL/chat/send")
                
                Log.d("API", "URL para enviar mensaje de chat: $url")
                
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                
                // Usar exactamente los nombres que el backend espera según documentación
                val jsonBody = JSONObject().apply {
                    put("user2_id", friendId)
                    put("message", message)  // Cambiado de "txt_message" a "message"
                }
                
                // Imprimir para depuración
                Log.d("API", "Enviando mensaje: ${jsonBody.toString()}")
                
                // Enviamos el cuerpo JSON
                connection.outputStream.use { os ->
                    os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                    os.flush()
                }
                
                val responseCode = connection.responseCode
                Log.d("API", "Código de respuesta sendChatMessage: $responseCode")
                
                if (responseCode in 200..299) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("API", "Respuesta sendChatMessage: $response")
                    return@withContext JSONObject(response)
                } else {
                    val errorStream = connection.errorStream
                    val errorBody = errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
                    Log.e("API", "Error en sendChatMessage: $errorBody")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("API", "Excepción en sendChatMessage: ${e.message}", e)
                return@withContext null
            }
        }
    }

    /**
     * Envía un mensaje de chat con contenido compartido (playlist, canción, etc).
     * 
     * @param friendId ID del amigo destinatario
     * @param message Texto del mensaje
     * @param sharedContent JSON en formato string con la información del contenido compartido
     * @param context Contexto para obtener el token de autenticación
     * @return ID del mensaje enviado o null si hay error
     */
    suspend fun sendChatMessageWithSharedContent(
        friendId: String, 
        message: String, 
        sharedContent: String,
        context: Context
    ): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("auth_token", null)
                
                if (token.isNullOrEmpty()) {
                    Log.e("API", "Token no disponible para enviar mensaje con contenido compartido")
                    return@withContext null
                }
                
                // URL para enviar mensajes
                val url = URL("$BASE_URL/chat/send")
                
                Log.d("API", "URL para enviar mensaje con contenido compartido: $url")
                
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                
                // Crear el cuerpo del mensaje con el contenido compartido
                val jsonBody = JSONObject().apply {
                    put("user2_id", friendId)
                    put("message", message)
                    put("shared_content", sharedContent)
                }
                
                // Imprimir para depuración
                Log.d("API", "Enviando mensaje con contenido: ${jsonBody}")
                
                // Enviamos el cuerpo JSON
                connection.outputStream.use { os ->
                    os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                    os.flush()
                }
                
                val responseCode = connection.responseCode
                Log.d("API", "Código de respuesta sendChatMessageWithSharedContent: $responseCode")
                
                if (responseCode in 200..299) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("API", "Respuesta sendChatMessageWithSharedContent: $response")
                    return@withContext JSONObject(response)
                } else {
                    val errorStream = connection.errorStream
                    val errorBody = errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
                    Log.e("API", "Error en sendChatMessageWithSharedContent: $errorBody")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("API", "Excepción en sendChatMessageWithSharedContent: ${e.message}", e)
                return@withContext null
            }
        }
    }

    /**
     * Obtiene información básica de una playlist para compartir.
     * 
     * @param playlistId ID de la playlist
     * @param context Contexto para obtener el token de autenticación
     * @return Información básica de la playlist o null si hay error
     */
    suspend fun getPlaylistShareInfo(playlistId: String, context: Context): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("auth_token", null)
                
                if (token.isNullOrEmpty()) {
                    Log.e("API", "Token no disponible para obtener información de playlist")
                    return@withContext null
                }
                
                // URL para obtener información básica de la playlist
                val url = URL("$BASE_URL/playlists/$playlistId/share-info")
                
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $token")
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                
                val responseCode = connection.responseCode
                
                if (responseCode in 200..299) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("API", "Información de playlist para compartir: $response")
                    return@withContext JSONObject(response)
                } else {
                    val errorStream = connection.errorStream
                    val errorBody = errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
                    Log.e("API", "Error obteniendo información de playlist: $errorBody")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("API", "Excepción obteniendo información de playlist: ${e.message}", e)
                return@withContext null
            }
        }
    }


    /**
     * Obtiene la valoración dada por un usuario específico a una playlist.
     * GET /api/ratingPlaylist/:id/user-rating?userId=X
     *
     * @param playlistId ID de la playlist
     * @param userId ID del usuario
     * @return Float con la valoración (0.0 si no hay valoración) o null si hay error
     */
    suspend fun getUserRating(playlistId: String, userId: String): Float? = withContext(Dispatchers.IO) {
        try {
            val endpoint = "ratingPlaylist/$playlistId/user-rating?userId=$userId"
            val url = URL("$BASE_URL/$endpoint")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            println("Código de respuesta (getUserRating): $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Leer la respuesta
                val response = connection.inputStream.bufferedReader().readText()
                println("Respuesta del servidor (getUserRating): $response")

                // Parsear la respuesta JSON para obtener el valor de userRating
                val jsonResponse = JSONObject(response)
                val userRating = jsonResponse.optDouble("userRating", 0.0).toFloat()
                Log.d("Rating", "Valoración del usuario: $userRating")

                return@withContext userRating
            } else {
                println("Error al obtener valoración: código $responseCode")
                connection.errorStream?.bufferedReader()?.use { errorReader ->
                    val errorResponse = errorReader.readText()
                    println("Mensaje de error: $errorResponse")
                }
                return@withContext null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error al obtener valoración del usuario: ${e.message}")
            return@withContext null
        }
    }

    /**
     * Invita a un colaborador a una playlist.
     * POST /collaborators/invite { playlistId, userId }
     */
    suspend fun inviteCollaborator(
        playlistId: String,
        userId: String,
        context: Context
    ): JSONObject? = withContext(Dispatchers.IO) {

        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        val url = URL("$BASE_URL/collaborators/invite")
        (url.openConnection() as HttpURLConnection).run {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            doOutput = true
            connectTimeout = 15000
            readTimeout = 15000

            val body = JSONObject().apply {
                put("playlistId", playlistId)
                put("userId", userId)
            }.toString()

            outputStream.use { it.write(body.toByteArray()) }
            if (responseCode in 200..299) {
                return@withContext inputStream.bufferedReader().readText().let(::JSONObject)
            }
            return@withContext null
        }
    }

    /**
     * Obtiene invitaciones pendientes de una playlist.
     * GET /collaborators/{playlistId}/pending
     */
    suspend fun getPendingInvitations(
        playlistId: String,
        context: Context
    ): JSONObject? = withContext(Dispatchers.IO) {

        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        val url = URL("$BASE_URL/collaborators/$playlistId/pending")
        (url.openConnection() as HttpURLConnection).run {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 10000
            readTimeout = 10000

            if (responseCode in 200..299) {
                return@withContext inputStream.bufferedReader().readText().let(::JSONObject)
            }
            return@withContext null
        }
    }

    /**
     * Acepta una invitación.
     * POST /collaborators/accept { playlistId }
     */
    suspend fun acceptCollaboration(
        playlistId: String,
        context: Context
    ): JSONObject? = withContext(Dispatchers.IO) {

        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        val url = URL("$BASE_URL/collaborators/accept")
        (url.openConnection() as HttpURLConnection).run {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            doOutput = true
            connectTimeout = 15000
            readTimeout = 15000

            val body = JSONObject().apply {
                put("playlistId", playlistId)
            }.toString()

            outputStream.use { it.write(body.toByteArray()) }
            if (responseCode in 200..299) {
                return@withContext inputStream.bufferedReader().readText().let(::JSONObject)
            }
            return@withContext null
        }
    }

    /**
     * Rechaza una invitación.
     * POST /collaborators/reject { playlistId }
     */
    suspend fun rejectCollaboration(
        playlistId: String,
        context: Context
    ): JSONObject? = withContext(Dispatchers.IO) {

        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        val url = URL("$BASE_URL/collaborators/reject")
        (url.openConnection() as HttpURLConnection).run {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            doOutput = true
            connectTimeout = 15000
            readTimeout = 15000

            val body = JSONObject().apply {
                put("playlistId", playlistId)
            }.toString()

            outputStream.use { it.write(body.toByteArray()) }
            if (responseCode in 200..299) {
                return@withContext inputStream.bufferedReader().readText().let(::JSONObject)
            }
            return@withContext null
        }
    }

    /**
     * Elimina a un colaborador de la playlist.
     * POST /collaborators/remove { playlistId, userId }
     */
    suspend fun removeCollaborator(
        playlistId: String,
        userId: String,
        context: Context
    ): JSONObject? = withContext(Dispatchers.IO) {

        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        val url = URL("$BASE_URL/collaborators/remove")
        (url.openConnection() as HttpURLConnection).run {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            doOutput = true
            connectTimeout = 15000
            readTimeout = 15000

            val body = JSONObject().apply {
                put("playlistId", playlistId)
                put("userId", userId)
            }.toString()

            outputStream.use { it.write(body.toByteArray()) }
            if (responseCode in 200..299) {
                return@withContext inputStream.bufferedReader().readText().let(::JSONObject)
            }
            return@withContext null
        }
    }

    /**
     * Obtiene todos los colaboradores de una playlist.
     * GET /collaborators/{playlistId}
     */
    suspend fun getCollaborators(
        playlistId: String,
        context: Context
    ): JSONObject? = withContext(Dispatchers.IO) {

        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        val url = URL("$BASE_URL/collaborators/$playlistId")
        (url.openConnection() as HttpURLConnection).run {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 10000
            readTimeout = 10000

            if (responseCode in 200..299) {
                return@withContext inputStream.bufferedReader().readText().let(::JSONObject)
            }
            return@withContext null
        }
    }

    /**
     * Obtiene playlists donde el usuario es colaborador.
     * GET /collaborators/playlists-for-user/{userId}
     */
    suspend fun getCollaborativePlaylists(
        userId: String
    ): JSONArray? = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/collaborators/playlists-for-user/$userId")
        (url.openConnection() as HttpURLConnection).run {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000

            if (responseCode in 200..299) {
                return@withContext inputStream.bufferedReader().readText().let(::JSONArray)
            }
            return@withContext null
        }
    }



}