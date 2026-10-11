package com.vextreme.vexlife.android.data.vexstream

import org.junit.Assert.assertEquals
import org.junit.Test

class VexStreamRemoteProtocolTest {

    @Test
    fun hkdfMatchesDesktopProtocolVector() {
        val shared =
            ByteArray(32) { index ->
                (index + 1).toByte()
            }
        val derived =
            VexStreamRemoteProtocol.hkdf(
                shared = shared,
                requestRef = "remote.fixture.001",
            )

        assertEquals(
            "c7b38bfac5c6643c44cec0ee47e2b2feff38a3ae80a8ea209a3f75a2547afb6c",
            derived.toHex(),
        )
    }

    @Test
    fun requestAndAadBytesAreStable() {
        assertEquals(
            listOf(
                "VEXSTREAM_REMOTE_REQUEST_AAD_V1",
                "vexstream.home.fixture",
                "vexlife.fixture",
                "remote.fixture",
            ).joinToString("\n"),
            String(
                VexStreamRemoteProtocol.requestAad(
                    homeRef = "vexstream.home.fixture",
                    deviceRef = "vexlife.fixture",
                    requestRef = "remote.fixture",
                ),
                Charsets.UTF_8,
            ),
        )

        assertEquals(
            listOf(
                "VEXSTREAM_REMOTE_RESPONSE_AAD_V1",
                "vexstream.home.fixture",
                "remote.fixture",
            ).joinToString("\n"),
            String(
                VexStreamRemoteProtocol.responseAad(
                    homeRef = "vexstream.home.fixture",
                    requestRef = "remote.fixture",
                ),
                Charsets.UTF_8,
            ),
        )
    }

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
}

// [VXG RealForever]
