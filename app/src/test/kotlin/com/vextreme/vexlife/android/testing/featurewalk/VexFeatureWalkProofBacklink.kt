package com.vextreme.vexlife.android.testing.featurewalk

import java.lang.reflect.Method

/**
 * One machine-checked source locator from a Feature-Walk Checkpoint to an existing focused test.
 *
 * The backlink is not a PASS receipt. Test execution remains the proof producer.
 */
data class VexFeatureWalkProofBacklink(
    val checkpointRef: String,
    val testClass: Class<*>,
    val testMethodName: String,
) {
    init {
        require(checkpointRef.isNotBlank()) {
            "Feature-Walk proof checkpoint ref must not be blank"
        }
        require(testMethodName.isNotBlank()) {
            "Feature-Walk proof test method must not be blank"
        }
    }

    val testLocator: String
        get() =
            "${testClass.name}#$testMethodName"

    fun resolveMethod(): Method =
        testClass.getDeclaredMethod(
            testMethodName,
        )
}

// [VXG RealForever]
