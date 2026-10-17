package com.vextreme.vexlife.android.testing.featurewalk

import java.lang.reflect.Method
import kotlin.reflect.KClass

/**
 * One machine-checked source locator from a Feature-Walk Checkpoint to an existing focused test.
 *
 * The backlink is not a PASS receipt. Test execution remains the proof producer.
 */
data class VexFeatureWalkProofBacklink(
    val checkpointRef: String,
    val testClass: KClass<*>,
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
            "${testClass.qualifiedName}#$testMethodName"

    fun resolveMethod(): Method =
        testClass.java.getDeclaredMethod(
            testMethodName,
        )
}

// [VXG RealForever]
