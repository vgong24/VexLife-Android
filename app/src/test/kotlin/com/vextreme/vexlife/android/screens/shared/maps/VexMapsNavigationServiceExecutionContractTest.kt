package com.vextreme.vexlife.android.screens.shared.maps

import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlinx.coroutines.CoroutineScope
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexMapsNavigationServiceExecutionContractTest {
    @Test
    fun serviceUsesVexExecutionRootInsteadOfOwningAPrivateCoroutineScope() {
        val fields =
            VexMapsNavigationService::class.java
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
    }
}

// [VXG RealForever][VEXARCHITECTURE][NAVIGATION-SERVICE][VEXEXECUTION][CURRENT-MAIN]
