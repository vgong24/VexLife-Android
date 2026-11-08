package com.vextreme.vexlife.android.screens.shared.maps

import com.vextreme.vexlife.android.behavior.VexService
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexMapsNavigationServiceExecutionContractTest {
    @Test
    fun serviceBindsCurrentThroughVexServiceAndUsesCanonicalExecutionCustody() {
        val serviceClass =
            VexMapsNavigationService::class.java

        assertTrue(
            VexService::class.java
                .isAssignableFrom(
                    serviceClass,
                ),
        )

        val fields =
            serviceClass
                .declaredFields

        assertTrue(
            fields.any { field ->
                VexExecutionRoot::class.java
                    .isAssignableFrom(
                        field.type,
                    )
            },
        )
        assertFalse(
            fields.any { field ->
                CoroutineScope::class.java
                    .isAssignableFrom(
                        field.type,
                    )
            },
        )
        assertFalse(
            fields.any { field ->
                Job::class.java
                    .isAssignableFrom(
                        field.type,
                    )
            },
        )
    }
}

// [VXG RealForever][VEXARCHITECTURE][NAVIGATION-SERVICE][VEXEXECUTION]
