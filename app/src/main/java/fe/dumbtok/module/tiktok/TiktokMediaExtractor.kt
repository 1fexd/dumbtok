package fe.dumbtok.module.tiktok

import androidx.annotation.VisibleForTesting
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import fe.gson.extension.json.array.elementsOrNull
import fe.gson.extension.json.element.stringOrNull
import fe.gson.extension.json.`object`.asArrayOrNull
import fe.gson.extension.json.`object`.asBooleanOrNull
import fe.gson.extension.json.`object`.asDoubleOrNull
import fe.gson.extension.json.`object`.asObjectOrNull
import fe.gson.extension.json.`object`.asStringOrNull
import org.jsoup.Jsoup
import java.io.InputStream

class TiktokMediaExtractor {

    fun parseHtmlPage(htmlStream: InputStream, url: String): JsonObject? {
        val document = Jsoup.parse(htmlStream, Charsets.UTF_8.name(), url)
        val script = document.getElementById("__UNIVERSAL_DATA_FOR_REHYDRATION__")

        val data = script?.data()?.let {
            JsonParser.parseString(it) as? JsonObject
        }

        return data?.asObjectOrNull("__DEFAULT_SCOPE__")
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun handleImagePost(imagePost: JsonObject): List<String>? {
        return imagePost.asArrayOrNull("images")
            ?.elementsOrNull<JsonObject>()
            ?.mapNotNull { image ->
                image?.asObjectOrNull("imageURL")
                    ?.asArrayOrNull("urlList")
                    ?.firstOrNull()
                    ?.stringOrNull()
            }
    }

    fun handleMusic(music: JsonObject): Pair<String, Double>? {
        val url = music.asStringOrNull("playUrl") ?: return null
        val duration = music.asObjectOrNull("preciseDuration")
            ?.asDoubleOrNull("preciseDuration") ?: return null

        return url to duration
    }

    fun handleVideo(video: JsonObject): Pair<String, Boolean>? {
        val url = video.asStringOrNull("playAddr") ?: return null
        val hasOriginalAudio = video.asObjectOrNull("claInfo")?.asBooleanOrNull("hasOriginalAudio") == true

        return url to hasOriginalAudio
    }

    fun extract(root: JsonObject): Post? {
        val itemStruct = root.asObjectOrNull("webapp.video-detail")
            ?.asObjectOrNull("itemInfo")
            ?.asObjectOrNull("itemStruct")

        val postId = itemStruct?.asStringOrNull("id") ?: return null
        val music = itemStruct.asObjectOrNull("music")
            ?.let { handleMusic(it) }
            ?.let { (url, duration) -> PostMusic(url, duration) }

        val imagePost = itemStruct.asObjectOrNull("imagePost")
        if (imagePost != null) {
            return handleImagePost(imagePost)
                ?.let { ImagePost(postId, imageUrls = it, music = music) }
        }

        return itemStruct.asObjectOrNull("video")
            ?.let { handleVideo(it) }
            ?.let { (url, hasOriginalAudio) ->
                VideoPost(postId, videoUrl = url, hasOriginalAudio = hasOriginalAudio)
            }
    }
}
