package fe.dumbtok.tiktok

class TikTokUrlService {
    private val shortUrlRegex = Regex("^https?:\\/\\/vm\\.tiktok\\.com\\/(.+)\\/?\$")
    private val fullUrlRegex = Regex("^https?:\\/\\/(?:www\\.)?tiktok\\.com\\/@(.+)\\/(.+)\\/(\\d+).*\$")

    fun isFullUrl(url: String): Triple<String, String, String>? {
        val (_, username, type, postId) = fullUrlRegex.matchEntire(url)?.groupValues ?: return null
        return Triple(username, type, postId)
    }

    fun isShortUrl(url: String): Boolean {
        return shortUrlRegex.matchEntire(url)?.groupValues?.get(1) != null
    }

    fun formatPostUrl(postId: String): String {
        return "https://tiktok.com/@i/video/$postId"
    }
}
