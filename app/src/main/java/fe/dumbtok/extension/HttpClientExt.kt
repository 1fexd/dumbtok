package fe.dumbtok.extension

import io.ktor.client.*
import io.ktor.client.statement.*
import io.ktor.http.*


suspend fun HttpClient.followRedirectsOrNull(
    urlString: String,
    maxCount: Int = 5,
    block: suspend HttpClient.(String) -> HttpResponse,
): HttpResponse? {
    var mutUrl = urlString
    var count = 0
    do {
        val response = block(mutUrl)
        if (response.status.value in 300..399) {
            val location = response.headers[HttpHeaders.Location]!!
            mutUrl = location
            count++
        } else {
            return response
        }
    } while (count < maxCount)

    return null
}

suspend fun HttpClient.followRedirects(
    urlString: String,
    maxCount: Int = 5,
    block: suspend HttpClient.(String) -> HttpResponse,
): HttpResponse {
    return followRedirectsOrNull(urlString, maxCount, block)!!
}
