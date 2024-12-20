package fe.dumbtok.tiktok

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.tableOf
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlin.test.Test

internal class TiktokApiServiceMediaExtractorTest {
    private val extractor = TiktokMediaExtractor()

    private val imagePostJson = """
    {
      "images": [
        {
          "imageURL": {
            "urlList": [
              "https://dumbtok.app/first",
              "https://dumbtok.app/second"
            ]
          }
        }
      ]
    }
    """.trimIndent()
    private val imagePostJson2 = """
    {
      "images": [
        {
          "imageURL": {
            "urlList": [
              "https://dumbtok.app/first",
              "https://dumbtok.app/second"
            ]
          }
        },
        {
          "imageURL": {
            "urlList": [
              "https://dumbtok2.app/first",
              "https://dumbtok2.app/second"
            ]
          }
        }
      ]
    }
    """.trimIndent()

    private fun parse(json: String): JsonObject {
        return JsonParser.parseString(json) as JsonObject
    }

    @Test
    fun `test extract image urls`() {
        tableOf("json", "result")
            .row<String, List<String>?>("{}", null)
            .row("""{"images": {}}""", null)
            .row("""{"images": []}""", emptyList())
            .row(imagePostJson, listOf("https://dumbtok.app/first"))
            .row(imagePostJson2, listOf("https://dumbtok.app/first", "https://dumbtok2.app/first"))
            .forAll { json, result ->
                assertThat(extractor.handleImagePost(parse(json))).isEqualTo(result)
            }
    }
}
