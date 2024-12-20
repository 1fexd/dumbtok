package fe.dumbtok.tiktok

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import assertk.tableOf
import org.junit.Test

internal class TikTokUrlServiceTest {
    private val service = TikTokUrlService()

    @Test
    fun `test short url matcher`() {
        assertThat(service.isShortUrl("https://vm.tiktok.com/Test123Test/")).isTrue()
        assertThat(service.isShortUrl("https://www.tiktok.com/@dumbtok/video/1234567890")).isFalse()
    }

    @Test
    fun `test full url matcher`() {
        assertThat(service.isFullUrl("https://vm.tiktok.com/Test123Test/")).isNull()

        tableOf("url", "username", "type", "postId")
            .row(
                "https://www.tiktok.com/@dumbtok/video/1234567890",
                "dumbtok",
                "video",
                "1234567890"
            )
            .row(
                "https://www.tiktok.com/@dumbtok/photo/1234567890?_r=1&checksum=9b29993171b3948703ae7bf0ed921e71&link_reflow_popup_iteration_sharer=%7B%22click_empty_to_play%22%3A1%2C%22profile_clickable%22%3A1%2C%22follow_to_play_duration%22%3A-1%2C%22dynamic_cover%22%3A1%7D&sec_user_id=fake&share_app_id=fsa&share_item_id=s21&share_link_id=Hello&sharer_language=en&social_share_type=14&tt_from=whatsapp&ug_btm=f%2Cb2001&ug_photo_idx=0",
                "dumbtok",
                "photo",
                "1234567890"
            )
            .forAll { url, username, type, postId ->
                val result = service.isFullUrl(url)

                assertThat(result).isNotNull()
                assertThat(result!!)
                    .all {
                        prop("first") { it.first }.isEqualTo(username)
                        prop("second") { it.second }.isEqualTo(type)
                        prop("third") { it.third }.isEqualTo(postId)
                    }
            }
    }
}
