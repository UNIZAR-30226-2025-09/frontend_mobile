package eina.unizar.es

import android.os.Build
import eina.unizar.es.utils.ApiTestUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.R])
class AlbumTest {

    private lateinit var apiUtils: ApiTestUtils

    @Before
    fun setup() {
        apiUtils = ApiTestUtils()
        runBlocking {
            apiUtils.login()
        }
    }

    @Test
    fun getAlbumsAndPlaylistsListTest() {
        runBlocking {
            val (code, _) = apiUtils.get("/playlists")
            assertTrue("El código debe ser 200 (éxito)", code == 200)
        }
    }


    @Test
    fun getAlbumDetailsTest() {
        runBlocking {
            // 1. Obtener la lista de playlists/álbumes
            val (listCode, listResponse) = apiUtils.get("/playlists")
            if (listCode == 200) {
                // 2. Buscar el primer ítem que sea álbum
                val items = when (listResponse) {
                    is org.json.JSONArray -> listResponse
                    is org.json.JSONObject -> if (listResponse.has("items")) {
                        listResponse.getJSONArray("items")
                    } else {
                        null
                    }
                    else -> null
                }

                val albumId = items?.let {
                    for (i in 0 until it.length()) {
                        val item = it.getJSONObject(i)
                        if (item.optBoolean("esAlbum", true)) {
                            return@let item.getInt("id")
                        }
                    }
                    null
                }

                if (albumId != null) {
                    // 3. Obtener detalles del álbum encontrado
                    val (detailCode, _) = apiUtils.get("/playlists/$albumId")
                    assertEquals("Debe devolver 200 para un álbum existente", 200, detailCode)
                } else {
                    fail("No se encontraron álbumes en la respuesta")
                }
            } else {
                fail("No se pudo obtener la lista de playlists (código $listCode)")
            }
        }
    }
}