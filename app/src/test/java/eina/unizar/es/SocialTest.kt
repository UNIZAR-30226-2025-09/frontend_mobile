package eina.unizar.es

import android.os.Build
import eina.unizar.es.utils.ApiTestUtils
import kotlinx.coroutines.runBlocking
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
}
