package eina.unizar.es

import android.os.Build
import eina.unizar.es.utils.ApiTestUtils
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.R])
class SongTest {

    private lateinit var apiUtils: ApiTestUtils

    @Before
    fun setup() {
        apiUtils = ApiTestUtils()
        runBlocking {
            apiUtils.login()
        }
    }

    @Test
    fun getSongsListTest() {
        val (code, response) = apiUtils.get("/songs")

        when (code) {
            200 -> {
                assertTrue("La respuesta debe ser un JSONArray", response is JSONArray)
                val songs = response as JSONArray
                assertTrue("Debe haber al menos una canción", songs.length() > 0)

                val firstSong = songs.getJSONObject(0)
                assertNotNull("ID de la canción", firstSong.optInt("id"))
                assertNotNull("Nombre de la canción", firstSong.optString("name"))
                assertNotNull("Duración de la canción", firstSong.optInt("duration"))
            }
            404 -> {
                assertTrue("La respuesta debe ser un JSONObject", response is JSONObject)
                assertEquals("No hay canciones disponibles.", (response as JSONObject).optString("message"))
            }
            else -> fail("Código de respuesta inesperado: $code")
        }
    }

    @Test
    fun getSongDetailsTest() {
        // Primero obtener lista de canciones
        val (listCode, listResponse) = apiUtils.get("/songs")

        if (listCode == 200 && listResponse is JSONArray && listResponse.length() > 0) {
            val songId = listResponse.getJSONObject(0).getInt("id")

            // Obtener detalles de la canción
            val (detailCode, detailResponse) = apiUtils.get("/songs/$songId")

            assertEquals(200, detailCode)
            assertTrue("Detalles debe ser un JSONObject", detailResponse is JSONObject)

            val songDetails = detailResponse as JSONObject
            assertTrue("La canción debería tener ID", songDetails.has("id"))
            assertTrue("La canción debería tener nombre", songDetails.has("name"))
            assertTrue("La canción debería tener duración", songDetails.has("duration"))

            // Verificar artista si existe
            if (songDetails.has("artist")) {
                assertTrue("Artista debe ser un objeto", songDetails.get("artist") is JSONObject)
            }
        }
    }
}