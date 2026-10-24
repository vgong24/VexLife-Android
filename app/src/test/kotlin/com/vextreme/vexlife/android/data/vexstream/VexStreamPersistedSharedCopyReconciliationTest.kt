package com.vextreme.vexlife.android.data.vexstream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamPersistedSharedCopyReconciliationTest {
    @Test
    fun twentyThousandUnchangedReceiptsStayOnMetadataOnlyPath() {
        val count =
            20_000
        val copies =
            (0 until count)
                .map { index ->
                    downloaded(
                        index = index,
                        receipt =
                            receipt(
                                index,
                            ),
                    )
                }
        val snapshots =
            copies
                .mapIndexed { index, localCopy ->
                    localCopy.localMediaRef to
                        snapshot(
                            localCopy = localCopy,
                            receipt =
                                receipt(
                                    index,
                                ),
                        )
                }
                .toMap()

        val reconciliation =
            reconcilePersistedSharedCopies(
                localCopies = copies,
                snapshots = snapshots,
            )

        assertEquals(
            count,
            reconciliation.reusableCopies.size,
        )
        assertTrue(
            reconciliation
                .verificationCandidates
                .isEmpty(),
        )
        assertEquals(
            copies.map {
                it.localMediaRef
            },
            reconciliation
                .reusableCopies
                .keys
                .toList(),
        )
    }

    @Test
    fun legacySevenHundredEightyFiveCopiesRequireOneTimeByteVerification() {
        val count =
            785
        val copies =
            (0 until count)
                .map { index ->
                    downloaded(
                        index = index,
                        receipt = null,
                    )
                }
        val snapshots =
            copies
                .mapIndexed { index, localCopy ->
                    localCopy.localMediaRef to
                        snapshot(
                            localCopy = localCopy,
                            receipt =
                                receipt(
                                    index,
                                ),
                        )
                }
                .toMap()

        val reconciliation =
            reconcilePersistedSharedCopies(
                localCopies = copies,
                snapshots = snapshots,
            )

        assertTrue(
            reconciliation
                .reusableCopies
                .isEmpty(),
        )
        assertEquals(
            count,
            reconciliation
                .verificationCandidates
                .size,
        )
        assertEquals(
            copies,
            reconciliation
                .verificationCandidates
                .map {
                    it.localCopy
                },
        )
    }

    @Test
    fun fiveThousandCopiesVerifyOnlyTheStaleReceiptSubset() {
        val count =
            5_000
        val staleIndexes =
            setOf(
                0,
                997,
                1_994,
                2_991,
                3_988,
                4_985,
            )
        val copies =
            (0 until count)
                .map { index ->
                    downloaded(
                        index = index,
                        receipt =
                            receipt(
                                index,
                            ),
                    )
                }
        val snapshots =
            copies
                .mapIndexed { index, localCopy ->
                    val currentReceipt =
                        if (
                            index in
                            staleIndexes
                        ) {
                            receipt(
                                index = index,
                                generationOffset =
                                    100_000L,
                            )
                        } else {
                            receipt(
                                index,
                            )
                        }

                    localCopy.localMediaRef to
                        snapshot(
                            localCopy =
                                localCopy,
                            receipt =
                                currentReceipt,
                        )
                }
                .toMap()

        val reconciliation =
            reconcilePersistedSharedCopies(
                localCopies = copies,
                snapshots = snapshots,
            )

        assertEquals(
            count -
                staleIndexes.size,
            reconciliation
                .reusableCopies
                .size,
        )
        assertEquals(
            staleIndexes.size,
            reconciliation
                .verificationCandidates
                .size,
        )
        assertEquals(
            staleIndexes
                .map {
                    copies[
                        it
                    ].localMediaRef
                },
            reconciliation
                .verificationCandidates
                .map {
                    it.localCopy
                        .localMediaRef
                },
        )
    }

    @Test
    fun knownInvalidSnapshotsAreRejectedBeforeByteVerification() {
        val copies =
            (0 until 5)
                .map { index ->
                    downloaded(
                        index = index,
                        receipt =
                            receipt(
                                index,
                            ),
                    )
                }
        val staleCandidate =
            copies[0]
        val snapshots =
            mapOf(
                staleCandidate.localMediaRef to
                    snapshot(
                        localCopy =
                            staleCandidate,
                        receipt =
                            receipt(
                                index = 0,
                                generationOffset =
                                    10_000L,
                            ),
                    ),
                copies[2].localMediaRef to
                    snapshot(
                        localCopy =
                            copies[2],
                        receipt =
                            receipt(
                                2,
                            ),
                        pending = true,
                    ),
                copies[3].localMediaRef to
                    snapshot(
                        localCopy =
                            copies[3],
                        receipt =
                            receipt(
                                3,
                            ),
                        mediaSha256 =
                            sha256(
                                30_003,
                            ),
                    ),
                copies[4].localMediaRef to
                    snapshot(
                        localCopy =
                            copies[4],
                        receipt =
                            receipt(
                                4,
                            ),
                        byteLength =
                            copies[4]
                                .byteLength +
                                1L,
                    ),
            )

        val reconciliation =
            reconcilePersistedSharedCopies(
                localCopies = copies,
                snapshots = snapshots,
            )

        assertTrue(
            reconciliation
                .reusableCopies
                .isEmpty(),
        )
        assertEquals(
            listOf(
                staleCandidate,
            ),
            reconciliation
                .verificationCandidates
                .map {
                    it.localCopy
                },
        )
    }

    private fun downloaded(
        index: Int,
        receipt:
            VexStreamLocalCopyVerificationReceipt?,
    ): VexStreamLocalCopyState.Downloaded =
        VexStreamLocalCopyState.Downloaded(
            mediaSha256 =
                sha256(
                    index,
                ),
            byteLength =
                4_096L +
                    index,
            localMediaRef =
                "content://media/external_primary/audio/media/" +
                    (index + 1),
            verificationReceipt =
                receipt,
        )

    private fun snapshot(
        localCopy:
            VexStreamLocalCopyState.Downloaded,
        receipt:
            VexStreamLocalCopyVerificationReceipt,
        pending: Boolean = false,
        mediaSha256:
            VexStreamMediaSha256 =
            localCopy.mediaSha256,
        byteLength: Long =
            localCopy.byteLength,
    ): VexStreamSharedCopySnapshot =
        VexStreamSharedCopySnapshot(
            localMediaRef =
                localCopy.localMediaRef,
            mediaSha256 =
                mediaSha256,
            byteLength =
                byteLength,
            pending =
                pending,
            verificationReceipt =
                receipt,
        )

    private fun receipt(
        index: Int,
        generationOffset: Long = 0L,
    ): VexStreamLocalCopyVerificationReceipt =
        VexStreamLocalCopyVerificationReceipt(
            mediaStoreVersion =
                "external-primary-v1",
            generationModified =
                generationOffset +
                    index,
        )

    private fun sha256(
        index: Int,
    ): VexStreamMediaSha256 =
        VexStreamMediaSha256(
            index
                .toString(
                    radix = 16,
                )
                .padStart(
                    length = 64,
                    padChar = '0',
                ),
        )
}

// [VXG RealForever]
