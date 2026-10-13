package com.vextreme.vexlife.android.screens.shared.maps

import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexMapsPackReplacementRecoveryTest {
    @Test
    fun committedCurrentWinsAndInterruptedArtifactsAreDiscarded() {
        val files =
            tempPackFiles()

        files.current.writeText("committed")
        files.staging.writeText("uncommitted")
        files.backup.writeText("previous")

        recoverVexMapsInterruptedPackReplacement(
            current = files.current,
            staging = files.staging,
            backup = files.backup,
        )

        assertEquals(
            "committed",
            files.current.readText(),
        )
        assertFalse(files.staging.exists())
        assertFalse(files.backup.exists())
    }

    @Test
    fun previousCommittedPackIsRestoredWhenCommitBoundaryWasNotReached() {
        val files =
            tempPackFiles()

        files.staging.writeText("replacement")
        files.backup.writeText("previous")

        recoverVexMapsInterruptedPackReplacement(
            current = files.current,
            staging = files.staging,
            backup = files.backup,
        )

        assertTrue(files.current.isFile)
        assertEquals(
            "previous",
            files.current.readText(),
        )
        assertFalse(files.staging.exists())
        assertFalse(files.backup.exists())
    }

    @Test
    fun firstImportStagingIsNotPromotedAfterInterruption() {
        val files =
            tempPackFiles()

        files.staging.writeText("uncommitted")

        recoverVexMapsInterruptedPackReplacement(
            current = files.current,
            staging = files.staging,
            backup = files.backup,
        )

        assertFalse(files.current.exists())
        assertFalse(files.staging.exists())
        assertFalse(files.backup.exists())
    }

    private fun tempPackFiles(): PackFiles {
        val directory =
            createTempDirectory(
                prefix = "vexmaps-pack-recovery-",
            )
                .toFile()
                .apply {
                    deleteOnExit()
                }

        fun file(
            name: String,
        ): File =
            File(
                directory,
                name,
            ).apply {
                deleteOnExit()
            }

        return PackFiles(
            current =
                file(
                    "current.pack",
                ),
            staging =
                file(
                    "current.pack.importing",
                ),
            backup =
                file(
                    "current.pack.previous",
                ),
        )
    }

    private data class PackFiles(
        val current: File,
        val staging: File,
        val backup: File,
    )
}

// [VXG RealForever]
