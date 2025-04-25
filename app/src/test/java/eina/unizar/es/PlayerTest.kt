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
class PlayerTest {

    private lateinit var apiUtils: ApiTestUtils
    private var songId: String = ""

    @Before
    fun setup() {
        apiUtils = ApiTestUtils()
        runBlocking {
            apiUtils.login()

            // Obtener una canción para las pruebas
            val (code, response) = apiUtils.get("/songs")
            assertEquals(200, code)

            // Verificar el tipo de respuesta
            when (response) {
                is JSONObject -> {
                    if (response.has("items")) {
                        val items = response.getJSONArray("items")
                        if (items.length() > 0) {
                            songId = items.getJSONObject(0).getString("id")
                        } else {
                            fail("No hay canciones disponibles")
                        }
                    } else {
                        fail("La respuesta no contiene el campo 'items'")
                    }
                }
                is JSONArray -> {
                    if (response.length() > 0) {
                        songId = response.getJSONObject(0).getString("id")
                    } else {
                        fail("No hay canciones disponibles")
                    }
                }
                else -> fail("Tipo de respuesta inesperado")
            }
        }
    }

    @Test
    fun getSongDetailsTest() = runBlocking {
        val (code, response) = apiUtils.get("/player/details/$songId")

        assertEquals(200, code)
        assertTrue("La respuesta debe ser un JSONObject", response is JSONObject)
        assertEquals(songId, (response as JSONObject).optString("id"))
    }

    @Test
    fun playSongTest() = runBlocking {
        val (code, response) = apiUtils.post("/player/play/$songId", JSONObject())

        assertEquals(200, code)
        assertTrue("La respuesta debe ser un JSONObject", response is JSONObject)
        assertTrue("Debe indicar que está reproduciendo",
            (response as JSONObject).optBoolean("isPlaying"))
    }

    @Test
    fun pauseSongTest() = runBlocking {
        val (code, response) = apiUtils.post("/player/pause/$songId", JSONObject())

        assertEquals(200, code)
        assertTrue("La respuesta debe ser un JSONObject", response is JSONObject)
        assertFalse("Debe indicar que está pausado",
            (response as JSONObject).optBoolean("isPlaying"))
    }
}