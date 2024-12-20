package fe.dumbtok.extension

import android.net.Uri
import io.ktor.http.Url

fun Url.toUri(): Uri? {
    return Uri.parse(toString())
}

fun Uri.toUrl(): Url {
    return Url(toString())
}
