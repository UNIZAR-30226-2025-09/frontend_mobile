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
class ArtistTest {

    private lateinit var apiUtils: ApiTestUtils

    @Before
    fun setup() {
        apiUtils = ApiTestUtils()
        runBlocking {
            apiUtils.login()
        }
    }

    @Test
    fun getArtistsListTest() {
        val (code, response) = apiUtils.get("/artist/artists")

        when (code) {
            200 -> {
                // Ahora esperamos un JSONArray directamente
                assertTrue("La respuesta debe ser un JSONArray", response is JSONArray)
                val artists = response as JSONArray

                assertTrue("Debe haber al menos un artista", artists.length() > 0)

                val firstArtist = artists.getJSONObject(0)
                assertNotNull("ID del artista", firstArtist.optInt("id"))
                assertNotNull("Nombre del artista", firstArtist.optString("name"))
                assertNotNull("Foto del artista", firstArtist.optString("photo"))
            }
            404 -> {
                assertTrue("La respuesta 404 debe ser un JSONObject", response is JSONObject)
                assertEquals("No hay artistas disponibles.", (response as JSONObject).optString("message"))
            }
            else -> fail("Código de respuesta inesperado: $code")
        }
    }

    @Test
    fun getArtistDetailsTest() {
        // Primero obtener la lista de artistas
        val (listCode, listResponse) = apiUtils.get("/artist/artists")

        if (listCode == 200 && listResponse is JSONArray && listResponse.length() > 0) {
            val artistId = listResponse.getJSONObject(0).getInt("id")

            // Obtener detalles del artista
            val (detailCode, detailResponse) = apiUtils.get("/artist/$artistId")

            assertEquals(200, detailCode)
            assertTrue("Detalles debe ser un JSONObject", detailResponse is JSONObject)
        }
    }

    @Test
    fun getArtistNotFoundTest() {
        val (code, response) = apiUtils.get("/artist/999999")

        assertEquals(404, code)
        assertTrue("Respuesta debe ser JSONObject", response is JSONObject)
        assertEquals("Artista no encontrado", (response as JSONObject).optString("message"))
    }
}