package fe.dumbtok.tiktok


import assertk.assertThat
import assertk.assertions.isEqualTo
import fe.dumbtok.TestStorageBackend
import io.ktor.client.engine.mock.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

internal class TikTokServiceTest {

    @Test
    fun `test resolve short link`(): Unit = runBlocking {
        val data = arrayOf(
            Triple("https://vm.tiktok.com/Dummy", "https://www.tiktok.com/@dumbtok/video/1234567890", "1234567890"),
            Triple("https://vm.tiktok.com/Dummy2", "https://www.tiktok.com/@dumbtok/photo/0987654321", "0987654321")
        )

        val mockEngine = MockEngine { request ->
            val urlString = request.url.toString()

            val (_, redirectUrl, _) = data.first { (shortUrl, _, _) -> shortUrl == urlString }
            respondRedirect(location = redirectUrl)
        }

        val mockService = TikTokService(engine = mockEngine, storage = TestStorageBackend)

        for ((postId, _, resultUrl) in data) {
            assertThat(mockService.resolveShortLink(postId)).isEqualTo(resultUrl)
        }
    }
}
