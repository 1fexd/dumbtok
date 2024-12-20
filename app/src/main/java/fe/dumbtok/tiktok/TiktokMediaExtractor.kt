package fe.dumbtok.tiktok

import androidx.annotation.VisibleForTesting
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import fe.gson.extension.json.array.elementsOrNull
import fe.gson.extension.json.element.stringOrNull
import fe.gson.extension.json.`object`.asArrayOrNull
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

    @VisibleForTesting(otherwise = VisibleForTesting.Companion.PRIVATE)
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

    fun extract(root: JsonObject): TiktokPostMedia? {
        val itemStruct = root.asObjectOrNull("webapp.video-detail")
            ?.asObjectOrNull("itemInfo")
            ?.asObjectOrNull("itemStruct")

        val postId = itemStruct?.asStringOrNull("id") ?: return null
        val imagePost = itemStruct.asObjectOrNull("imagePost")
        if (imagePost != null) {
            return handleImagePost(imagePost)
                ?.let { TiktokImagePost(postId, it) }
        }

        return itemStruct
            .asObjectOrNull("video")
            ?.asStringOrNull("playAddr")
            ?.let { TiktokVideoPost(postId, it) }
    }
}
