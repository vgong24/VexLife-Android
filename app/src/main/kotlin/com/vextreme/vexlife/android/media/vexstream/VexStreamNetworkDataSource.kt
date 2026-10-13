package com.vextreme.vexlife.android.media.vexstream

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.vextreme.vexlife.android.data.vexstream.VexStreamConnectionStore
import com.vextreme.vexlife.android.data.vexstream.VexStreamPinnedTls
import com.vextreme.vexlife.android.data.vexstream.VexStreamRemoteTransport
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URLEncoder

/**
 * Media3 network byte source for the remembered VexStream pairing.
 *
 * MediaItems use an opaque vexstream:// URI. The real LAN host, bearer token and certificate pin are
 * resolved here from app-private connection state so credentials never enter MediaItem metadata.
 */
internal class VexStreamNetworkDataSource(
    private val connectionStore: VexStreamConnectionStore,
    private val remoteTransport: VexStreamRemoteTransport,
) : BaseDataSource(
    true,
) {
    private var openedUri: Uri? = null
    private var connection: HttpURLConnection? = null
    private var input: InputStream? = null
    private var opened = false
    private var remainingBytes: Long = C.LENGTH_UNSET.toLong()

    override fun open(
        dataSpec: DataSpec,
    ): Long {
        transferInitializing(
            dataSpec,
        )

        val paired =
            connectionStore.load()
                ?: throw IOException(
                    "No paired VexStream connection is available",
                )
        val trackRef =
            dataSpec.uri.lastPathSegment
                ?.takeIf { it.isNotBlank() }
                ?: throw IOException(
                    "VexStream media reference missing",
                )

        val mediaPath =
            "v1/media/" +
                URLEncoder.encode(
                    trackRef,
                    Charsets.UTF_8.name(),
                ).replace(
                    "+",
                    "%20",
                )

        var openedDirect: HttpURLConnection? = null
        var openedInput: InputStream? = null
        var contentLength = -1L

        try {
            val http =
                VexStreamPinnedTls.open(
                    baseUrl = paired.endpointBaseUrl(),
                    certificateSha256 = paired.certificateSha256,
                    path = mediaPath,
                    token = paired.token,
                )

            dataSpec.httpRequestHeaders.forEach { (name, value) ->
                if (!name.equals("Authorization", ignoreCase = true)) {
                    http.setRequestProperty(
                        name,
                        value,
                    )
                }
            }

            if (
                dataSpec.position > 0L ||
                dataSpec.length != C.LENGTH_UNSET.toLong()
            ) {
                val end =
                    if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
                        ""
                    } else {
                        (
                            dataSpec.position +
                                dataSpec.length -
                                1L
                        ).toString()
                    }

                http.setRequestProperty(
                    "Range",
                    "bytes=${dataSpec.position}-$end",
                )
            }

            val status =
                http.responseCode
            if (
                status != HttpURLConnection.HTTP_OK &&
                status != HttpURLConnection.HTTP_PARTIAL
            ) {
                http.disconnect()
                throw VexStreamHttpStatusException(
                    status,
                )
            }

            openedDirect = http
            openedInput = http.inputStream
            contentLength =
                http.getHeaderFieldLong(
                    "Content-Length",
                    -1L,
                )
        } catch (error: Exception) {
            if (
                error is VexStreamHttpStatusException ||
                !paired.remoteReady
            ) {
                throw IOException(
                    "VexStream media request failed",
                    error,
                )
            }

            openedDirect?.disconnect()
            openedDirect = null
            openedInput =
                remoteTransport.openRangeStream(
                    connection = paired,
                    path = mediaPath,
                    start = dataSpec.position,
                    expectedLength =
                        dataSpec.length
                            .takeIf {
                                it !=
                                    C.LENGTH_UNSET
                                        .toLong()
                            },
                )
        }

        connection = openedDirect
        input = openedInput
        openedUri = dataSpec.uri
        opened = true

        remainingBytes =
            when {
                dataSpec.length != C.LENGTH_UNSET.toLong() ->
                    dataSpec.length

                contentLength >= 0L ->
                    contentLength

                else ->
                    C.LENGTH_UNSET.toLong()
            }

        transferStarted(
            dataSpec,
        )
        return remainingBytes
    }

    override fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Int {
        if (length == 0) {
            return 0
        }
        if (remainingBytes == 0L) {
            return C.RESULT_END_OF_INPUT
        }

        val maxRead =
            if (remainingBytes == C.LENGTH_UNSET.toLong()) {
                length
            } else {
                minOf(
                    length.toLong(),
                    remainingBytes,
                ).toInt()
            }

        val read =
            input
                ?.read(
                    buffer,
                    offset,
                    maxRead,
                )
                ?: C.RESULT_END_OF_INPUT

        if (read < 0) {
            return C.RESULT_END_OF_INPUT
        }

        if (remainingBytes != C.LENGTH_UNSET.toLong()) {
            remainingBytes -= read.toLong()
        }
        bytesTransferred(
            read,
        )
        return read
    }

    override fun getUri(): Uri? =
        openedUri

    override fun getResponseHeaders(): Map<String, List<String>> =
        connection
            ?.headerFields
            ?.filterKeys { it != null }
            ?.mapKeys { it.key.orEmpty() }
            ?: emptyMap()

    override fun close() {
        try {
            input?.close()
        } finally {
            input = null
            connection?.disconnect()
            connection = null
            openedUri = null
            remainingBytes = C.LENGTH_UNSET.toLong()

            if (opened) {
                opened = false
                transferEnded()
            }
        }
    }

    class Factory(
        context: Context,
    ) : DataSource.Factory {
        private val connectionStore =
            VexStreamConnectionStore(
                context.applicationContext,
            )
        private val remoteTransport =
            VexStreamRemoteTransport(
                connectionStore,
            )

        override fun createDataSource(): DataSource =
            VexStreamNetworkDataSource(
                connectionStore = connectionStore,
                remoteTransport = remoteTransport,
            )
    }
}

private class VexStreamHttpStatusException(
    val status: Int,
) : IOException(
    "VexStream media HTTP $status",
)

// [VXG RealForever]
