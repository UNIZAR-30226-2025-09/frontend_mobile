package eina.unizar.es

import android.os.Build
import androidx.media3.common.util.Log
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
class SongLikeTest {

    private lateinit var apiUtils: ApiTestUtils
    private var songId: String = "1"

    @Before
    fun setup() {
        apiUtils = ApiTestUtils()
        runBlocking {
            apiUtils.login()

            if (songId.isEmpty()) {
                fail("No se encontró ninguna canción sin like para el test.")
            }
        }
    }

    @Test
    fun toggleLikeTest() {
        val requestData = JSONObject().apply {
            put("user_id",1) // Usa el userId autenticado
            println("ID del usuario: " + ApiTestUtils.getUserId())
        }

        // Primer like
        val (code1, response1) = apiUtils.post(
            "/song_like/$songId/likeUnlike",
            requestData
        )

        // Imprimir para depuración
        println("Code 1: $code1")
        println("Response 1: $response1")

        assertEquals(200, code1)
        assertTrue("Primera respuesta debe ser JSONObject", response1 is JSONObject)
        assertTrue("Debe indicar like=true", (response1 as JSONObject).optBoolean("liked"))

        // Segundo toggle (unlike)
        val (code2, response2) = apiUtils.post(
            "/song_like/$songId/likeUnlike",
            requestData
        )

        // Imprimir para depuración
        println("Code 2: $code2")
        println("Response 2: $response2")

        assertEquals(200, code2)
        assertTrue("Segunda respuesta debe ser JSONObject", response2 is JSONObject)
        assertFalse("Debe indicar like=false", (response2 as JSONObject).optBoolean("liked"))
    }
}

