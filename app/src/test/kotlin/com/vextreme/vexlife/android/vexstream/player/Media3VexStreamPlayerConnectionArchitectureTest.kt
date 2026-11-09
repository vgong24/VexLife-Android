package com.vextreme.vexlife.android.vexstream.player

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Media3VexStreamPlayerConnectionArchitectureTest {
    @Test
    fun inboundConnectionOwnsItsExecutionCustodyInsteadOfUsingRootDirectOwnerRetention() {
        assertTrue(
            VexExecution::class.java
                .isAssignableFrom(
                    Media3VexStreamPlayerConnection::class.java,
                ),
        )
        assertFalse(
            Media3VexStreamPlayerConnection::class.java
                .declaredFields
                .any { field ->
                    field.type ==
                        VexExecutionRoot::class.java
                },
        )
    }
}

// [VXG RealForever][VEXSTREAM][PLAYER-PARTNER][EXECUTION][TEST]
