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
class SocialTest {

    private val apiUtils = ApiTestUtils()
    private lateinit var currentUser: TestUser
    private lateinit var friendUser: TestUser

    data class TestUser(
        val email: String,
        val password: String,
        var id: String = "",
        var token: String = ""
    )

    private fun authHeaders(token: String) = mapOf("Authorization" to "Bearer $token")

    @Before
    fun setup() = runBlocking {
        currentUser = createTestUser("testuser")
        friendUser = createTestUser("testfriend")
    }

    private suspend fun createTestUser(prefix: String): TestUser {
        val timestamp = System.currentTimeMillis()
        val user = TestUser(
            email = "$prefix$timestamp@test.com",
            password = "Testpass123!"
        )

        val registerBody = JSONObject().apply {
            put("nickname", "$prefix$timestamp")
            put("password", user.password)
            put("mail", user.email)
        }

        val (regCode, _) = apiUtils.post("/user/register", registerBody)
        if (regCode != 201) fail("Error registrando usuario $prefix")

        val loginBody = JSONObject().apply {
            put("mail", user.email)
            put("password", user.password)
        }

        val (loginCode, loginResponse) = apiUtils.post("/user/login", loginBody)
        if (loginCode != 200 || loginResponse !is JSONObject) {
            fail("Login falló para $prefix. Código: $loginCode, Respuesta: $loginResponse")
        }

        if (loginResponse != null) {
            user.id = loginResponse.optString("id").ifEmpty {
                loginResponse.optJSONObject("user")?.optString("id") ?: ""
            }
        }
        if (loginResponse != null) {
            user.token = loginResponse.optString("token").ifEmpty {
                loginResponse.optJSONObject("user")?.optString("token") ?: ""
            }
        }

        if (user.id.isEmpty() || user.token.isEmpty()) {
            fail("No se pudo obtener ID o token para $prefix. Respuesta: $loginResponse")
        }

        return user
    }

    @Test
    fun sendFriendRequestTest() = runBlocking {
        val requestData = JSONObject().apply {
            put("user2_id", friendUser.id)
        }

        println("Enviando solicitud de ${currentUser.id} a ${friendUser.id}")
        val (code, response) = apiUtils.post("/social/send", requestData, authHeaders(currentUser.token))

        println("Respuesta enviar solicitud: Código $code - $response")

        when (code) {
            200, 201 -> assertTrue("Respuesta debe ser JSONObject", response is JSONObject)
            400 -> assertTrue("Solicitud duplicada", response is JSONObject)
            else -> fail("Error inesperado: $code - $response")
        }
    }

    @Test
    fun acceptFriendRequestTest() = runBlocking {
        val requestData = JSONObject().apply {
            put("user2_id", friendUser.id)
        }

        val (sendCode, _) = apiUtils.post("/social/send", requestData, authHeaders(currentUser.token))
        assertTrue("Falló enviar solicitud", sendCode in listOf(200, 201, 400))

        val acceptData = JSONObject().apply {
            put("user1_id", currentUser.id)
        }

        println("Aceptando solicitud de ${currentUser.id} por ${friendUser.id}")
        val (acceptCode, acceptResponse) = apiUtils.post("/social/accept", acceptData, authHeaders(friendUser.token))

        println("Respuesta aceptar solicitud: Código $acceptCode - $acceptResponse")

        when (acceptCode) {
            200 -> assertTrue(acceptResponse is JSONObject)
            400 -> assertTrue("Solicitud ya aceptada", acceptResponse is JSONObject)
            else -> fail("Error al aceptar: $acceptCode - $acceptResponse")
        }
    }

    @Test
    fun getFriendRequestsTest() {
        // 1. Hacer petición usando el usuario ya creado en el setup
        val (code, response) = apiUtils.post(
            "/social/getReceivedFriendRequests",
            body = JSONObject(),
            headers = authHeaders(currentUser.token)
        )

        // 2. Verificar respuesta básica
        when (code) {
            200 -> {
                // Caso con solicitudes recibidas
                assertTrue("Respuesta debe ser JSONObject", response is JSONObject)
                val json = response as JSONObject

                // Verificar estructura mínima
                assertTrue(json.has("receivedRequests"))
                assertTrue(json.has("count"))

                val requests = json.getJSONArray("receivedRequests")
                for (i in 0 until requests.length()) {
                    val request = requests.getJSONObject(i)
                    assertTrue(request.has("friendId"))
                    assertTrue(request.has("nickname"))
                    assertTrue(request.has("state"))
                }

            }

            404 -> {
                // Caso sin solicitudes
                assertTrue("Respuesta debe ser JSONObject", response is JSONObject)
                assertEquals(
                    "No hay solicitudes pendientes",
                    (response as JSONObject).getString("message")
                )
            }

            else -> fail("Código inesperado: $code - $response")
        }
    }


    @Test
    fun getFriendsListTest() {
        // 1. Hacer la petición con el usuario ya autenticado
        val (code, rawResponse) = apiUtils.post(
            "/social/getFriendsList",
            headers = authHeaders(currentUser.token)
        )

        // 2. Verificar que el código de respuesta es 200
        if (code != 200) {
            fail("Se esperaba código 200 pero se recibió $code. Respuesta: ${rawResponse?.take(200)}")
        }

        // 3. Verificar que no sea un error HTML
        if (rawResponse.toString().startsWith("<!DOCTYPE")) {
            fail("Error del servidor (HTML recibido): ${rawResponse?.take(500)}")
        }

        // 4. Parsear y devolver el JSON
        try {
            JSONObject(rawResponse.toString()).also {
                println("Respuesta JSON exitosa: ${it.toString(2)}")
            }
        } catch (e: Exception) {
            fail("Error al parsear JSON: ${e.message}\nContenido: ${rawResponse?.take(500)}")
        }
    }

    @Test
    fun getFriendsListWithoutTokenShouldFail() {
        val (code, raw) = apiUtils.post("/social/getFriendsList", headers = emptyMap())
        assertEquals(401, code)

        val errorJson = JSONObject(raw)
        assertEquals("Token no proporcionado", errorJson.getString("error"))
    }

    @Test
    fun getFriendsListWithInvalidTokenShouldFail() {
        val (code, raw) = apiUtils.post(
            "/social/getFriendsList",
            headers = mapOf("Authorization" to "Bearer token_invalido")
        )

        assertEquals(500, code)
    }



}
