package com.vextreme.vexlife.android.vexstream.repeat

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Directional Repeat intent ("repeat on" / "repeat off") for callers that may have formed the
 * request against older perception. The human Toggle keeps its existing meaning.
 */
class VexStreamRepeatDirectionalIntentTest {
    @Test
    fun enableFromDisabledRequestsRepeatAll() {
        val accepted =
            requireNotNull(
                familyIn(
                    VexStreamRepeatState.Disabled(
                        toggleAvailable =
                            true,
                    ),
                ).activate(
                    VexStreamRepeatControl.Enable,
                ),
            )

        assertSame(
            VexStreamRepeatEffect.RequestEnableAll,
            requireNotNull(
                accepted.realizationBegin,
            ).effect,
        )
    }

    @Test
    fun enableWhenAlreadyEnabledAuthorsNoAct() {
        assertNull(
            familyIn(
                VexStreamRepeatState.Enabled(
                    toggleAvailable =
                        true,
                ),
            ).activate(
                VexStreamRepeatControl.Enable,
            ),
        )
    }

    @Test
    fun disableFromEnabledRequestsDisable() {
        val accepted =
            requireNotNull(
                familyIn(
                    VexStreamRepeatState.Enabled(
                        toggleAvailable =
                            true,
                    ),
                ).activate(
                    VexStreamRepeatControl.Disable,
                ),
            )

        assertSame(
            VexStreamRepeatEffect.RequestDisable,
            requireNotNull(
                accepted.realizationBegin,
            ).effect,
        )
    }

    @Test
    fun disableWhenAlreadyDisabledAuthorsNoAct() {
        assertNull(
            familyIn(
                VexStreamRepeatState.Disabled(
                    toggleAvailable =
                        true,
                ),
            ).activate(
                VexStreamRepeatControl.Disable,
            ),
        )
    }

    @Test
    fun directionalIntentRespectsProviderCapability() {
        assertNull(
            familyIn(
                VexStreamRepeatState.Disabled(
                    toggleAvailable =
                        false,
                ),
            ).activate(
                VexStreamRepeatControl.Enable,
            ),
        )
    }

    @Test
    fun directionalAndToggleShareOneRepeatRelationship() {
        val family =
            familyIn(
                VexStreamRepeatState.Disabled(
                    toggleAvailable =
                        true,
                ),
            )

        val enable =
            requireNotNull(
                family.activate(
                    VexStreamRepeatControl.Enable,
                ),
            )
        val toggle =
            requireNotNull(
                family.activate(
                    VexStreamRepeatControl.Toggle,
                ),
            )

        // Latest supersession only happens inside one maintained relationship.
        assertNull(
            enable.realizationBegin,
        )
        assertSame(
            VexStreamRepeatEffect.RequestEnableAll,
            requireNotNull(
                toggle.realizationBegin,
            ).effect,
        )
    }

    private fun familyIn(
        state:
            VexStreamRepeatState,
    ):
        VexStreamRepeatFamily =
        VexStreamRepeatFamily(
            initialState =
                state,
        )
}

// [VXG RealForever][VEXSTREAM][REPEAT][DIRECT-FAMILY][DIRECTIONAL-INTENT][PROOF]
