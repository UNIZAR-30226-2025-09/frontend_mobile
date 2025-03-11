package eina.unizar.es.data.model.network
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2/request/api" // Usa la IP local del backend
    //private const val BASE_URL = "http://164.90.160.181/request/api" // Usa la IP publica (nube) del backend

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
            println("⚠ Error al conectar con el backend: ${e.message}")
            null
        }
    }

    /**
     * Método para realizar una petición POST en segundo plano.
     * @param endpoint Ruta del recurso (ejemplo: "create-payment-intent").
     * @param jsonBody Cuerpo de la solicitud en formato JSON.
     * @return Respuesta en formato JSON o `null` si hay error.
     */
    suspend fun post(endpoint: String, jsonBody: JSONObject): String? = withContext(Dispatchers.IO) {
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
                connection.inputStream.bufferedReader().use { it.readText() } // Lee la respuesta correctamente
            } else {
                Log.e("ApiClient", "Error en la respuesta del servidor: código $responseCode")
                connection.errorStream?.bufferedReader()?.use { it.readText() } // Leer el mensaje de error si lo hay
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ApiClient", "Error de conexión con el backend: ${e.message}")
            null
        }
    }
}