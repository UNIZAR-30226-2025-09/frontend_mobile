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
class StripeTest {

    private lateinit var apiUtils: ApiTestUtils

    @Before
    fun setup() {
        apiUtils = ApiTestUtils()
        runBlocking {
            apiUtils.login()
        }
    }

    @Test
    fun createPaymentIntentTest() {
        val (code, response) = apiUtils.post("/stripe/create-payment-intent", JSONObject())

        assertEquals(200, code)
        assertTrue("Respuesta debe ser JSONObject", response is JSONObject)
        assertTrue("Debe contener clientSecret",
            (response as JSONObject).has("clientSecret"))
    }
}