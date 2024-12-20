package fe.dumbtok

import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import fe.dumbtok.extension.followRedirectsOrNull
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.text.toIntOrNull

internal class HttpClientFollowRedirectsTest {
    private fun redirectTestClient(isRedirect: MockRequestHandleScope.(Int) -> Boolean): HttpClient {
        val engine =  MockEngine {
            val requestNum = it.url.parameters["num"]?.toIntOrNull()!!
            val nextRequestNum = (requestNum + 1)

            val next = URLBuilder(it.url).apply {
                parameters["num"] = nextRequestNum.toString()
            }

            when {
                isRedirect(nextRequestNum) -> respondRedirect(location = next.buildString())
                else -> respondOk()
            }
        }

        return HttpClient(engine) {
            followRedirects = false
        }
    }

    @Test
    fun `test redirect following`(): Unit = runBlocking {
        val client = redirectTestClient { it == 2 }

        val response = client.followRedirectsOrNull(maxCount = 3, urlString = "https://dumbtok.app?num=0") {
            get(urlString = it)
        }

        assertThat(response).isNotNull()
    }

    @Test
    fun `test redirect max count respected`(): Unit = runBlocking {
        val client = redirectTestClient { true }

        val response = client.followRedirectsOrNull(maxCount = 3, urlString = "https://dumbtok.app?num=0") {
            get(urlString = it)
        }

        assertThat(response).isNull()
    }
}
