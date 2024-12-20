package fe.dumbtok.media3

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaLibraryInfo
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.HttpUtil
import androidx.media3.datasource.TransferListener
import com.google.common.base.Predicate
import com.google.common.io.ByteStreams
import com.google.common.net.HttpHeaders
import com.google.errorprone.annotations.CanIgnoreReturnValue
import fe.dumbtok.extension.toUri
import fe.dumbtok.extension.toUrl
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.CacheControl
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.util.toMap
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import kotlin.math.min

/**
 * Adapted version of OkHttpDataSource for Ktor
 */
class KtorHttpDataSource private constructor(
    private val httpClient: HttpClient,
    private val userAgent: String?,
    private val cacheControl: CacheControl?,
    private val defaultRequestProperties: HttpDataSource.RequestProperties?,
    private val contentTypePredicate: Predicate<String?>?,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseDataSource(true), HttpDataSource, CoroutineScope by CoroutineScope(dispatcher) {

    class Factory(
        private val httpClient: HttpClient,
        private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : HttpDataSource.Factory {

        private val defaultRequestProperties: HttpDataSource.RequestProperties = HttpDataSource.RequestProperties()

        private var userAgent: String? = null
        private var transferListener: TransferListener? = null
        private var cacheControl: CacheControl? = null
        private var contentTypePredicate: Predicate<String?>? = null

        @CanIgnoreReturnValue
        @UnstableApi
        override fun setDefaultRequestProperties(defaultRequestProperties: Map<String, String>): Factory {
            this.defaultRequestProperties.clearAndSet(defaultRequestProperties)
            return this
        }

        @CanIgnoreReturnValue
        @UnstableApi
        fun setUserAgent(userAgent: String?): Factory {
            this.userAgent = userAgent
            return this
        }

        /**
         * Sets the [CacheControl] that will be used.
         *
         *
         * The default is `null`.
         *
         * @param cacheControl The cache control that will be used.
         * @return This factory.
         */
        @CanIgnoreReturnValue
        @UnstableApi
        fun setCacheControl(cacheControl: CacheControl?): Factory {
            this.cacheControl = cacheControl
            return this
        }

        /**
         * Sets a content type [Predicate]. If a content type is rejected by the predicate then a
         * [InvalidContentTypeException] is thrown from [ ][KtorHttpDataSource.open].
         *
         *
         * The default is `null`.
         *
         * @param contentTypePredicate The content type [Predicate], or `null` to clear a
         * predicate that was previously set.
         * @return This factory.
         */
        @CanIgnoreReturnValue
        @UnstableApi
        fun setContentTypePredicate(contentTypePredicate: Predicate<String?>?): Factory {
            this.contentTypePredicate = contentTypePredicate
            return this
        }

        /**
         * Sets the [TransferListener] that will be used.
         *
         *
         * The default is `null`.
         *
         *
         * See [androidx.media3.datasource.DataSource.addTransferListener].
         *
         * @param transferListener The listener that will be used.
         * @return This factory.
         */
        @CanIgnoreReturnValue
        @UnstableApi
        fun setTransferListener(transferListener: TransferListener?): Factory {
            this.transferListener = transferListener
            return this
        }

        @UnstableApi
        override fun createDataSource(): KtorHttpDataSource {
            val dataSource = KtorHttpDataSource(
                httpClient,
                userAgent,
                cacheControl,
                defaultRequestProperties,
                contentTypePredicate,
                dispatcher
            )
            if (transferListener != null) {
                dataSource.addTransferListener(transferListener!!)
            }
            return dataSource
        }
    }

    private val requestProperties: HttpDataSource.RequestProperties = HttpDataSource.RequestProperties()

    private var dataSpec: DataSpec? = null
    private var response: HttpResponse? = null
    private var responseByteStream: InputStream? = null
    private var connectionEstablished = false
    private var bytesToRead: Long = 0
    private var bytesRead: Long = 0

    @UnstableApi
    override fun getUri(): Uri? {
        return response?.call?.request?.url?.toUri() ?: dataSpec?.uri
    }

    @UnstableApi
    override fun getResponseCode(): Int {
        return response?.status?.value ?: -1
    }

    @UnstableApi
    override fun getResponseHeaders(): Map<String, List<String>> {
        return response?.headers?.toMap() ?: emptyMap()
    }

    @UnstableApi
    override fun setRequestProperty(name: String, value: String) {
        requestProperties.set(name, value)
    }

    @UnstableApi
    override fun clearRequestProperty(name: String) {
        requestProperties.remove(name)
    }

    @UnstableApi
    override fun clearAllRequestProperties() {
        requestProperties.clear()
    }

    private suspend fun openSuspending(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        bytesRead = 0
        bytesToRead = 0
        transferInitializing(dataSpec)

        val response = makeRequest(dataSpec)
        this.response = response

        try {
            responseByteStream = response.bodyAsChannel().toInputStream()
        } catch (e: IOException) {
            throw HttpDataSource.HttpDataSourceException.createForIOException(
                e,
                dataSpec,
                HttpDataSource.HttpDataSourceException.TYPE_OPEN
            )
        }

        val responseCode = response.status

        // Check for a valid response code.
        if (!responseCode.isSuccess()) {
            if (responseCode == HttpStatusCode.Companion.RequestedRangeNotSatisfiable) {
                val documentSize = HttpUtil.getDocumentSize(response.headers[HttpHeaders.CONTENT_RANGE])
                if (dataSpec.position == documentSize) {
                    connectionEstablished = true
                    transferStarted(dataSpec)
                    return if (dataSpec.length != C.LENGTH_UNSET.toLong()) dataSpec.length else 0
                }
            }

            var errorResponseBody = try {
                ByteStreams.toByteArray(responseByteStream!!)
            } catch (e: IOException) {
                Util.EMPTY_BYTE_ARRAY
            }

            val headers = response.headers
            closeConnectionQuietly()
            val cause = if (responseCode == HttpStatusCode.Companion.RequestedRangeNotSatisfiable) {
                DataSourceException(PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)
            } else {
                null
            }

            throw HttpDataSource.InvalidResponseCodeException(
                responseCode.value, responseCode.description, cause, headers.toMap(), dataSpec, errorResponseBody
            )
        }

        // Check for a valid content type.
        val mediaType = response.contentType()
        val contentType = mediaType?.toString() ?: ""
        if (contentTypePredicate != null && !contentTypePredicate.apply(contentType)) {
            closeConnectionQuietly()
            throw HttpDataSource.InvalidContentTypeException(contentType, dataSpec)
        }

        // If we requested a range starting from a non-zero position and received a 200 rather than a
        // 206, then the server does not support partial requests. We'll need to manually skip to the
        // requested position.
        val bytesToSkip = if (responseCode == HttpStatusCode.Companion.OK && dataSpec.position != 0L) dataSpec.position else 0

        // Determine the length of the data to be read, after skipping.
        bytesToRead = when {
            dataSpec.length != C.LENGTH_UNSET.toLong() -> dataSpec.length
            else -> {
                val contentLength = response.contentLength() ?: -1L
                when {
                    contentLength != -1L -> contentLength - bytesToSkip
                    else -> C.LENGTH_UNSET.toLong()
                }
            }
        }

        connectionEstablished = true
        transferStarted(dataSpec)

        try {
            skipFully(bytesToSkip, dataSpec)
        } catch (e: HttpDataSource.HttpDataSourceException) {
            closeConnectionQuietly()
            throw e
        }

        return bytesToRead
    }

    @UnstableApi
    @Throws(HttpDataSource.HttpDataSourceException::class)
    override fun open(dataSpec: DataSpec): Long {
        val job = async { openSuspending(dataSpec) }
        return job.asCompletableFuture().get()
    }

    @UnstableApi
    @Throws(HttpDataSource.HttpDataSourceException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        try {
            return readInternal(buffer, offset, length)
        } catch (e: IOException) {
            throw HttpDataSource.HttpDataSourceException.createForIOException(
                e, dataSpec!!, HttpDataSource.HttpDataSourceException.TYPE_READ
            )
        }
    }

    @UnstableApi
    override fun close() {
        if (connectionEstablished) {
            connectionEstablished = false
            transferEnded()
            closeConnectionQuietly()
        }
        response = null
        dataSpec = null
    }

    /** Establishes a connection.  */
    @Throws(HttpDataSource.HttpDataSourceException::class)
    private suspend fun makeRequest(dataSpec: DataSpec): HttpResponse {
        val position = dataSpec.position
        val length = dataSpec.length

        val url = dataSpec.uri.toUrl()
        val headers = mutableMapOf<String, String>()
        if (cacheControl != null) {
            headers.put(io.ktor.http.HttpHeaders.CacheControl, cacheControl.toString())
        }

        if (defaultRequestProperties != null) {
            headers.putAll(defaultRequestProperties.getSnapshot())
        }

        headers.putAll(requestProperties.getSnapshot())
        headers.putAll(dataSpec.httpRequestHeaders)

        return httpClient.request(url) {
            for (header in headers.entries) {
                header(header.key, header.value)
            }

            val rangeHeader = HttpUtil.buildRangeRequestHeader(position, length)
            if (rangeHeader != null) {
                header(HttpHeaders.RANGE, rangeHeader)
            }
            if (userAgent != null) {
                header(HttpHeaders.USER_AGENT, userAgent)
            }
            if (!dataSpec.isFlagSet(DataSpec.FLAG_ALLOW_GZIP)) {
                header(HttpHeaders.ACCEPT_ENCODING, "identity")
            }

            method = HttpMethod.Companion.parse(dataSpec.httpMethodString)
            if (dataSpec.httpBody != null) {
                setBody(dataSpec.httpBody)
            } else if (dataSpec.httpMethod == DataSpec.HTTP_METHOD_POST) {
                // OkHttp requires a non-null body for POST requests.
                setBody(Util.EMPTY_BYTE_ARRAY)
            }
        }
    }

    /**
     * Attempts to skip the specified number of bytes in full.
     *
     * @param bytesToSkip The number of bytes to skip.
     * @param dataSpec The [DataSpec].
     * @throws HttpDataSourceException If the thread is interrupted during the operation, or an error
     * occurs while reading from the source, or if the data ended before skipping the specified
     * number of bytes.
     */
    @Throws(HttpDataSource.HttpDataSourceException::class)
    private fun skipFully(bytesToSkip: Long, dataSpec: DataSpec) {
        var bytesToSkip = bytesToSkip
        if (bytesToSkip == 0L) {
            return
        }
        val skipBuffer = ByteArray(4096)
        try {
            while (bytesToSkip > 0) {
                val readLength = min(bytesToSkip.toDouble(), skipBuffer.size.toDouble()).toInt()
                val read = responseByteStream!!.read(skipBuffer, 0, readLength)
                if (Thread.currentThread().isInterrupted) {
                    throw InterruptedIOException()
                }

                if (read == -1) {
                    throw HttpDataSource.HttpDataSourceException(
                        dataSpec,
                        PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
                        HttpDataSource.HttpDataSourceException.TYPE_OPEN
                    )
                }

                bytesToSkip -= read.toLong()
                bytesTransferred(read)
            }
            return
        } catch (e: IOException) {
            if (e is HttpDataSource.HttpDataSourceException) {
                throw e
            } else {
                throw HttpDataSource.HttpDataSourceException(
                    dataSpec,
                    PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                    HttpDataSource.HttpDataSourceException.TYPE_OPEN
                )
            }
        }
    }

    /**
     * Reads up to `length` bytes of data and stores them into `buffer`, starting at index
     * `offset`.
     *
     *
     * This method blocks until at least one byte of data can be read, the end of the opened range
     * is detected, or an exception is thrown.
     *
     * @param buffer The buffer into which the read data should be stored.
     * @param offset The start offset into `buffer` at which data should be written.
     * @param readLength The maximum number of bytes to read.
     * @return The number of bytes read, or [C.RESULT_END_OF_INPUT] if the end of the opened
     * range is reached.
     * @throws IOException If an error occurs reading from the source.
     */
    @Throws(IOException::class)
    private fun readInternal(buffer: ByteArray?, offset: Int, readLength: Int): Int {
        var readLength = readLength
        if (readLength == 0) return 0

        if (bytesToRead != C.LENGTH_UNSET.toLong()) {
            val bytesRemaining = bytesToRead - bytesRead
            if (bytesRemaining == 0L) {
                return C.RESULT_END_OF_INPUT
            }

            readLength = min(readLength.toDouble(), bytesRemaining.toDouble()).toInt()
        }

        val read = responseByteStream!!.read(buffer, offset, readLength)
        if (read == -1) {
            return C.RESULT_END_OF_INPUT
        }

        bytesRead += read.toLong()
        bytesTransferred(read)
        return read
    }

    /** Closes the current connection quietly, if there is one.  */
    private fun closeConnectionQuietly() {
        if (response != null) {
            responseByteStream?.close()
        }
        responseByteStream = null
    }

    companion object {
        init {
            MediaLibraryInfo.registerModule("media3.datasource.ktor")
        }
    }
}
