package fe.dumbtok.module.tiktok

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
open class Post(val postId: String, val music: PostMusic?) : Parcelable

@Parcelize
class VideoPost(
    postId: String,
    music: PostMusic? = null,
    val videoUrl: String,
    val hasOriginalAudio: Boolean,
) : Post(postId, music) {

    override fun toString(): String {
        return "VideoPost(videoUrl='$videoUrl', hasOriginalAudio=$hasOriginalAudio)"
    }
}

@Parcelize
class ImagePost(
    postId: String,
    music: PostMusic? = null,
    val imageUrls: List<String>,
) : Post(postId, music) {

    override fun toString(): String {
        return "ImagePost(imageUrls=$imageUrls)"
    }
}

@Parcelize
class PostMusic(val url: String, val duration: Double) : Parcelable {

    override fun toString(): String {
        return "PostMusic(url='$url', duration=$duration)"
    }
}
