package fe.dumbtok.tiktok

sealed class TiktokPostMedia(val postId: String)

class TiktokVideoPost(
    postId: String,
    val videoUrl: String,
) : TiktokPostMedia(postId) {

}

class TiktokImagePost(
    postId: String,
    val imageUrls: List<String>,
) : TiktokPostMedia(postId) {

}
