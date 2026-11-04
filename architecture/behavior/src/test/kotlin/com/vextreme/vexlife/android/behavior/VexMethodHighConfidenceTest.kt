package com.vextreme.vexlife.android.behavior

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexMethodHighConfidenceTest {
    private class ExampleMethod(
        private val result:
            VexMethod.Result<
                String,
                Throwable?
            >,
    ) :
        VexMethod<
            String,
            Throwable?
        > {
        override suspend fun invoke():
            VexMethod.Result<
                String,
                Throwable?
            > =
            result
    }

    @Test
    fun contractOwnedBranchesCarryOutputsWithoutAnotherResultTaxonomy() =
        runBlocking {
            val success =
                ExampleMethod(
                    VexMethod.Result.Success(
                        output =
                            "ready",
                    ),
                )()
            val failure =
                ExampleMethod(
                    VexMethod.Result.Failure(
                        output =
                            IllegalStateException(
                                "rejected",
                            ),
                    ),
                )()

            val successResult =
                success as
                    VexMethod.Result.Success<String>
            assertEquals(
                "ready",
                successResult.output,
            )

            val failureResult =
                failure as
                    VexMethod.Result.Failure<Throwable?>
            assertEquals(
                "rejected",
                failureResult.output
                    ?.message,
            )
        }

    @Test
    fun resultBranchesAreExhaustiveWithoutElse() {
        val success:
            VexMethod.Result<
                String,
                Throwable?
            > =
            VexMethod.Result.Success(
                output =
                    "ready",
            )
        val failure:
            VexMethod.Result<
                String,
                Throwable?
            > =
            VexMethod.Result.Failure(
                output =
                    null,
            )
        val error:
            VexMethod.Result<
                String,
                Throwable?
            > =
            VexMethod.Result.Error(
                IllegalStateException(
                    "escaped",
                ),
            )

        assertEquals(
            "success:ready",
            describe(
                success,
            ),
        )
        assertEquals(
            "failure",
            describe(
                failure,
            ),
        )
        assertEquals(
            "error:escaped",
            describe(
                error,
            ),
        )
    }

    private fun describe(
        result:
            VexMethod.Result<
                String,
                Throwable?
            >,
    ):
        String =
        when (
            result
        ) {
            is VexMethod.Result.Success ->
                "success:" +
                    result.output

            is VexMethod.Result.Failure ->
                "failure"

            is VexMethod.Result.Error ->
                "error:" +
                    (
                        result.cause.message
                            ?: "unknown"
                    )
        }

    @Test
    fun errorCarriesEscapedThrowable() {
        val failure =
            IllegalStateException(
                "synthetic method escape",
            )
        val error =
            VexMethod.Result.Error(
                cause =
                    failure,
            )

        assertTrue(
            error.cause ===
                failure,
        )
    }
}

// [VXG RealForever]
