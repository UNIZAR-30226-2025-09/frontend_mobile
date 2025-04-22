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
class UserTest {

    private lateinit var apiUtils: ApiTestUtils

    @Before
    fun setup() {
        apiUtils = ApiTestUtils()
        runBlocking {
            apiUtils.login()
        }
    }

    @Test
    fun registerUserTest() {
        val userData = JSONObject().apply {
            put("nickname", "testuser")
            put("password", "testpass")
            put("mail", "test@test.com")
        }

        val (code, response) = apiUtils.post("/user/register", userData)

        when (code) {
            201 -> {
                assertTrue("Respuesta debe ser JSONObject", response is JSONObject)
                assertEquals("Usuario registrado con éxito", (response as JSONObject).optString("message"))
            }
            400, 409 -> {
                assertTrue("Respuesta de error debe ser JSONObject", response is JSONObject)
                assertTrue("Debe contener mensaje de error", (response as JSONObject).has("error"))
            }
            else -> fail("Código de respuesta inesperado: $code")
        }
    }

    @Test
    fun loginUserTest() {
        val credentials = JSONObject().apply {
            put("mail", "test@test.com")
            put("password", "testpass")
        }

        val (code, response) = apiUtils.post("/user/login", credentials)

        when (code) {
            200 -> {
                assertTrue("Respuesta debe ser JSONObject", response is JSONObject)
                assertTrue("Debe contener token", (response as JSONObject).has("token"))
            }
            401, 404 -> {
                assertTrue("Respuesta de error debe ser JSONObject", response is JSONObject)
                assertTrue("Debe contener mensaje de error", (response as JSONObject).has("error"))
            }
            else -> fail("Código de respuesta inesperado: $code")
        }
    }
    
    @Test
    fun getUserProfileTest() {
        runBlocking {
            val loginBody = JSONObject()
                .put("mail", "testuser@test.com")
                .put("password", "password")

            val (loginCode, loginResponse) = apiUtils.post("/user/login", loginBody)
            assertEquals(200, loginCode)
            val token = loginResponse?.getString("token")
            assertNotNull("Token no recibido", token)

            val headers = mapOf("Authorization" to "Bearer $token")
            val (code, response) = apiUtils.get("/user/profile", headers)

            assertEquals(200, code)
            assertTrue("Respuesta debe ser JSONObject", response is JSONObject)
            assertTrue("Debe contener datos de usuario", (response as JSONObject).has("nickname"))
        }
    }

}
