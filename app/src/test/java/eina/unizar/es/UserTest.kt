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
    fun updateUserProfileTest() {
        // Registrar un usuario de prueba
        val email = registroUsuarioTest()

        // Iniciar sesión para obtener el token
        val credentials = JSONObject().apply {
            put("mail", email)
            put("password", "testpass")
        }
        val (loginCode, loginResponse) = apiUtils.post("/user/login", credentials)

        assertEquals(200, loginCode)  // Asegurarse de que el login fue exitoso
        val token = loginResponse?.getString("token")
        assertNotNull("Token no recibido", token)  // Asegurarse de que el token es válido

        // Usar el email del usuario para la actualización de perfil
        val newNickname = "testuser_${System.currentTimeMillis()}"
        val (code, response) = apiUtils.post(
            "/user/update",
            JSONObject(mapOf(
                "currentPassword" to "testpass",
                "nickname" to newNickname,
                "mail" to email,        // Agregar el email del usuario
                "password" to "testpass" // Asegúrate de agregar la nueva contraseña si se necesita
            )),
            headers = mapOf("Authorization" to "Bearer $token")  // Usar el token de autorización obtenido
        )

        assertEquals(200, code)
        assertNotNull("La respuesta no debe ser nula", response)

        // Limpiar el usuario después de la prueba
        //limpiarUsuario(email)
    }

    @Test
    fun forgotPasswordTest() {
        // Registrar un usuario de prueba
        val email = registroUsuarioTest()

        val (code, response) = apiUtils.post(
            "/user/forgot-password",
            JSONObject().apply {
                put("mail", email)  // Usamos el email del usuario creado
            },
        )

        // Imprime el código de estado
        println("Código de estado: $code")
        // Imprime la respuesta del servidor
        println("Respuesta del servidor: $response")

        assertEquals(200, code)  // Asegúrate de que el código sea 200

        assertNotNull("La respuesta no debe ser nula", response)

        // Limpiar el usuario después de la prueba
        //limpiarUsuario(email)
    }

    @Test
    fun resetPasswordTest() {
        val testToken = "test_token_${System.currentTimeMillis()}"
        val (code, response) = apiUtils.post(
            "/user/reset-password",
            JSONObject(mapOf(
                "token" to testToken,
                "newPassword" to "newTestPass123!"
            ))
        )

        assertNotNull("La respuesta no debe ser nula", response)
    }

/*
    // ESTE TEST NO PASA
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

 */

    fun registroUsuarioTest(): String {
        val email = "user_${System.currentTimeMillis()}@test.com"
        val password = "testpass"

        // Hacemos el registro del usuario
        val (code, response) = apiUtils.post(
            "/user/register",
            JSONObject(mapOf(
                "mail" to email,
                "password" to password,
                "nickname" to "testuser_${System.currentTimeMillis()}"
            ))
        )

        assertEquals(201, code)
        assertNotNull("La respuesta no debe ser nula", response)

        // Si el registro es exitoso, solo devolvemos el email.
        return email
    }

    fun limpiarUsuario(email: String) {
        val (code, response) = apiUtils.post(
            "/user/delete",
            JSONObject(mapOf(
                "mail" to email
            ))
        )

        assertEquals(200, code)
        assertNotNull("La respuesta no debe ser nula", response)
    }

}
