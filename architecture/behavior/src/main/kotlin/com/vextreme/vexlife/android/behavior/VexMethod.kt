package com.vextreme.vexlife.android.behavior

/**
 * Reusable fully-bound executable method contract.
 *
 * VexMethod owns one executable attempt and its closed Result forms. Concrete domains supply only
 * the output carried by lawful Success / Failure. Error is formed inside the behavior foundation
 * when a Method escapes its lawful returned contract. VexMethod does not own semantic State,
 * execution custody, cancellation, provider observation, or domain-specific output vocabulary.
 */
interface VexMethod<
    out SuccessOutput,
    out FailureOutput,
> {
    suspend operator fun invoke():
        Result<
            SuccessOutput,
            FailureOutput
        >

    /**
     * Result of one Method attempt.
     *
     * Success / Failure are lawful returned outcomes. Error means the Method escaped its lawful
     * returned contract. Cancellation remains execution lifetime and is not a Result.
     *
     * Result is privately rooted, so the only terminal forms are Success, Failure, and Error.
     * Success / Failure remain public lawful-return carriers. Error construction is internal to the
     * behavior foundation so provider/domain code cannot manufacture an escaped-Method result.
     */
    sealed class Result<
        out SuccessOutput,
        out FailureOutput,
    > private constructor() {
        data class Success<
            out Output,
        >(
            val output:
                Output,
        ) :
            Result<
                Output,
                Nothing
            >()

        data class Failure<
            out Output,
        >(
            val output:
                Output,
        ) :
            Result<
                Nothing,
                Output
            >()

        @ConsistentCopyVisibility
        data class Error internal constructor(
            val cause:
                Throwable,
        ) :
            Result<
                Nothing,
                Nothing
            >()
    }
}

// [VXG RealForever]
