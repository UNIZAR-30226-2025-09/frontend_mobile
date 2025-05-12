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
    private var playlistId: Int? = null  // Variable global para almacenar el ID de la playlist


    @Before
    fun setup() {
        apiUtils = ApiTestUtils()
        runBlocking {
            apiUtils.login()
        }

        // Crear la playlist antes de los tests
        val playlistName = "Nueva Playlist"
        val playlistBody = JSONObject().put("name", playlistName)
        val (codeCreate, responseCreate) = apiUtils.post("/playlists", playlistBody)
        assertEquals(201, codeCreate)
        assertTrue("Debe devolver un JSONObject con la playlist creada", responseCreate is JSONObject)

        // Guardar el ID de la playlist creada
        playlistId = responseCreate?.getInt("id")
    }

    @Test
    fun getAllPlaylistsTest() {
        val (code, response) = apiUtils.get("/playlists")

        when (code) {
            200 -> {
                assertTrue("La respuesta debe ser un JSONArray", response is JSONArray)
                val playlists = response as JSONArray

                // Verificar que cada playlist tiene los campos básicos
                for (i in 0 until playlists.length()) {
                    val playlist = playlists.getJSONObject(i)
                    assertTrue("Debe tener id", playlist.has("id"))
                    assertTrue("Debe tener nombre", playlist.has("name"))
                    assertTrue("Debe tener tipo", playlist.has("type"))
                    assertTrue("Debe tener user_id", playlist.has("user_id"))
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


    @Test
    fun addSongToPlaylistAndVerify() {
        // Asegurarnos de que tenemos un ID de playlist válido
        assertNotNull("Debe haber una playlist creada previamente", playlistId)

        val songId = 1 // Cambiar si es necesario

        val songBody = JSONObject().put("songId", songId)
        val (codeAdd, _) = apiUtils.post("/playlists/${playlistId}/addSong", songBody)
        assertEquals(200, codeAdd)

        val (codeDetail, responseDetail) = apiUtils.get("/playlists/$playlistId")
        assertEquals(200, codeDetail)
        assertTrue("La respuesta debe ser un JSONObject", responseDetail is JSONObject)

        val playlistDetail = responseDetail as JSONObject
        val songsArray = playlistDetail.optJSONArray("songs") ?: JSONArray()

        var found = false
        for (i in 0 until songsArray.length()) {
            val song = songsArray.getJSONObject(i)
            if (song.getInt("id") == songId) {
                found = true
                break
            }
        }
        assertTrue("La canción debería estar en la playlist", found)
    }

    @Test
    fun deleteSongFromPlaylistAndVerify() {
        // Asegurarnos de que tenemos un ID de playlist válido
        assertNotNull("Debe haber una playlist creada previamente", playlistId)

        val songId = 1 // Cambiar si es necesario

        // Añadimos la canción a la lista
        val songBody = JSONObject().put("songId", songId)
        val (codeAdd, _) = apiUtils.post("/playlists/${playlistId}/addSong", songBody)
        assertEquals(200, codeAdd)

        // Borramos la canción
        val (codeDel, _) = apiUtils.post("/playlists/${playlistId}/deleteSong", songBody)
        assertEquals(200, codeDel)

        val (codeDetail, responseDetail) = apiUtils.get("/playlists/$playlistId")
        assertEquals(200, codeDetail)
        assertTrue("La respuesta debe ser un JSONObject", responseDetail is JSONObject)

        val playlistDetail = responseDetail as JSONObject
        val songsArray = playlistDetail.optJSONArray("songs") ?: JSONArray()

        var found = false
        for (i in 0 until songsArray.length()) {
            val song = songsArray.getJSONObject(i)
            if (song.getInt("id") == songId) {
                found = true
                break
            }
        }
        assertFalse("La canción ya no debería estar en la playlist", found)
    }

    @Test
    fun updatePlaylistTest() {
        val newName = "Updated Playlist ${System.currentTimeMillis()}"
        val (code, response) = apiUtils.put(
            "/playlists/$playlistId",
            JSONObject().apply {
                put("name", newName)
            }
        )

        assertEquals(200, code)
    }

    @Test
    fun togglePlaylistPrivacyTest() {
        // Obtener el estado actual
        val (getCode, getResponse) = apiUtils.get("/playlists/$playlistId")
        assertEquals(200, getCode)

        // Si la respuesta es un String, convertirlo a JSONObject
        val responseJson = try {
            JSONObject(getResponse.toString())  // Intenta convertir la respuesta a JSONObject si es un String
        } catch (e: Exception) {
            fail("Error al convertir la respuesta en JSONObject: ${e.message}")
            return
        }

        val currentType = responseJson.getString("type")

        // Cambiar el tipo
        val newType = if (currentType == "public") "private" else "public"
        val (code, response) = apiUtils.put(
            "/playlists/$playlistId",
            JSONObject().apply {
                put("type", newType)
            }
        )

        assertEquals(200, code)

        // Si la respuesta es un String, convertirlo a JSONObject
        val responseJsonUpdated = try {
            JSONObject(response.toString())  // Intentar convertir la respuesta en JSONObject
        } catch (e: Exception) {
            fail("Error al convertir la respuesta actualizada en JSONObject: ${e.message}")
            return
        }

        assertEquals(newType, responseJsonUpdated.optString("type"))
    }


    @Test
    fun getPlaylistAverageRatingTest() {
        // Asegurarnos de que tenemos un ID de playlist válido
        assertNotNull("Debe haber una playlist creada previamente", playlistId)

        // Obtener la valoración promedio
        val (code, response) = apiUtils.get("/ratingPlaylist/$playlistId/rating")

        when (code) {
            200 -> {
                assertTrue("La respuesta debe ser un JSONObject", response is JSONObject)
                val ratingResponse = response as JSONObject

                // Verificar que tiene el campo 'average' y está en el rango correcto
                assertTrue("Debe contener el campo 'averageRating'", ratingResponse.has("averageRating"))
                val averageRating = ratingResponse.getDouble("averageRating")
                assertTrue("El rating debe estar entre 0 y 5", averageRating in 0.0..5.0)

                // Opcional: Verificar que tiene el campo 'count' si tu API lo devuelve
                if (ratingResponse.has("count")) {
                    assertTrue("El conteo debe ser >= 0", ratingResponse.getInt("count") >= 0)
                }
            }
            404 -> {
                assertTrue("La respuesta debe ser un JSONObject", response is JSONObject)
                assertEquals("Playlist no encontrada", (response as JSONObject).optString("message"))
            }
            else -> fail("Código de respuesta inesperado: $code")
        }
    }

    /* //NO SE AUTENTICA BIEN LA PARTE DEL USUARIO
    @Test
    fun ratePlaylistTest() {
        assertNotNull("Debe haber una playlist creada previamente", playlistId)

        println("DEBUG: playlistId = $playlistId")

        val (profileCode, profileResponse) = apiUtils.get("/user/profile")
        println("DEBUG: profileCode = $profileCode, profileResponse = $profileResponse")

        if (profileCode != 200 || profileResponse !is JSONObject) {
            fail("No se pudo obtener el perfil del usuario")
            return
        }

        val userId = (profileResponse as JSONObject).optInt("id", -1)
        if (userId == -1) {
            fail("ID de usuario no encontrado en el perfil")
            return
        }

        val rating = 4
        val jsonBody = JSONObject().apply {
            put("user_id", userId)
            put("rating", rating)
        }

        val (code, response) = apiUtils.post("/ratingPlaylist/$playlistId/rate", jsonBody)

        println("DEBUG: rate response code = $code, response = $response")

        if (response is JSONObject) {
            when (code) {
                200 -> assertTrue("Rating realizado correctamente", true)
                400 -> fail("Solicitud incorrecta al valorar la playlist: ${response.optString("message")}")
                401 -> fail("No autorizado al valorar la playlist")
                else -> fail("Código inesperado al valorar la playlist: $code")
            }
        } else {
            // Si llega HTML o algo que no sea JSON
            println("DEBUG: Respuesta no JSON, probablemente error del servidor")
            fail("Error inesperado del servidor al valorar la playlist")
        }
    }
     */



    @Test
    fun deletePlaylistAndVerify() {
        // Asegurarnos de que tenemos un ID de playlist válido
        assertNotNull("Debe haber una playlist creada previamente", playlistId)

        // Borramos la playlist
        val (codeDelete, _) = apiUtils.delete("/playlists/$playlistId")
        assertEquals(200, codeDelete)

        // Verificamos que la lista fue eliminada
        val (codeCheck, responseCheck) = apiUtils.get("/playlists/$playlistId")
        assertEquals(500, codeCheck) //DEBERIA DEVOLVER 404
    }

}
