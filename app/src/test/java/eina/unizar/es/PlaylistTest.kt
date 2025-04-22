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
class PlaylistTest {

    private lateinit var apiUtils: ApiTestUtils

    @Before
    fun setup() {
        apiUtils = ApiTestUtils()
        runBlocking {
            apiUtils.login()
        }
    }

    @Test
    fun getPublicPlaylistsTest() {
        val (code, response) = apiUtils.get("/playlists")

        when (code) {
            200 -> {
                assertTrue("La respuesta debe ser un JSONArray", response is JSONArray)
                val playlists = response as JSONArray

                // Verificar que solo contiene playlists públicas
                for (i in 0 until playlists.length()) {
                    assertEquals("public", playlists.getJSONObject(i).getString("type"))
                }
            }
            404 -> {
                assertTrue("La respuesta debe ser un JSONObject", response is JSONObject)
                assertEquals("No hay playlists disponibles.", (response as JSONObject).optString("message"))
                fail("No se pudieron obtener las playlists")
            }
            else -> fail("Código de respuesta inesperado: $code")
        }
    }

    @Test
    fun getPlaylistDetailsTest() {
        // Primero obtener lista de playlists públicas
        val (listCode, listResponse) = apiUtils.get("/playlists")

        if (listCode == 200 && listResponse is JSONArray && listResponse.length() > 0) {
            val playlistId = listResponse.getJSONObject(0).getInt("id")

            // Obtener detalles de la playlist
            val (detailCode, detailResponse) = apiUtils.get("/playlists/$playlistId")

            assertEquals(200, detailCode)
            assertTrue("Detalles debe ser un JSONObject", detailResponse is JSONObject)

            val playlistDetails = detailResponse as JSONObject

            assertTrue("Canciones debe ser un array", playlistDetails.get("songs") is JSONArray)
        }
    }

    @Test
    fun getUserPlaylistsTest() {
        // Obtener playlists del usuario autenticado
        val (code, response) = apiUtils.get("/playlists/user")

        if (code == 200) {
            assertTrue("La respuesta debe ser un JSONArray", response is JSONArray)
            val playlists = response as JSONArray

            // Verificar que todas pertenecen al usuario
            val (profileCode, profileResponse) = apiUtils.get("/user/profile")
            if (profileCode == 200 && profileResponse is JSONObject) {
                val userId = profileResponse.getInt("id")

                for (i in 0 until playlists.length()) {
                    assertEquals(userId, playlists.getJSONObject(i).getInt("user_id"))
                }
            }
        } else if (code == 404) {
            assertTrue("La respuesta debe ser un JSONObject", response is JSONObject)
            assertEquals("No tienes playlists.", (response as JSONObject).optString("message"))
            fail("No se pudieron obtener las playlists del usuario")
        }
    }
}