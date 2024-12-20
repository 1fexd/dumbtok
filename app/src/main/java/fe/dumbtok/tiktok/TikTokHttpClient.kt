package fe.dumbtok.tiktok

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.cookies.*


fun TikTokHttpClient(engine: HttpClientEngine): HttpClient {
    return HttpClient(engine) {
        install(ContentEncoding)
        install(HttpCookies)
        install(UserAgent) {
            agent =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"
        }

        followRedirects = false
    }
}
