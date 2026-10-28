package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.relay.VexState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Patient-zero proof for the V2 Origin Walk.
 *
 * This test intentionally preserves why the current shape exists:
 *
 * 1. The first POC copied V1 and created ThemeModeFamily.Act values.
 * 2. That forced ThemeMode State to be interpreted once in Control->Act and again in Act->Effect.
 * 3. Removing the intermediate Act vocabulary lost no ThemeMode meaning.
 * 4. The surviving Act is the authored Behavior scenario itself: State + Control -> optional Effect
 *    + stay/admit.
 * 5. The real Debug Menu session path then proved Effect is optional: Light/Dark Toggle changes
 *    semantic State without earning a persistence/provider transformation.
 *
 * A fresh implementation should therefore extend State / Control / Behavior below, not recreate a
 * ThemeModeFamily.Act hierarchy merely because V1 has one.
 */
class VexBehaviorV2Test {
    private object ThemeModeFamily :
        VexBehaviorFamily<
            ThemeModeFamily
        > {
        /**
         * Family-scoped truth region.
         *
         * The nested path makes role and ownership visible without a separate registration list:
         * ThemeModeFamily.State.Dark cannot be mistaken for an unrelated top-level enum.
         */
        sealed interface State :
            VexBehaviorFamily.State<
                ThemeModeFamily
            >,
            VexState<
                State
            > {
            data object NotSet :
                State

            data object Light :
                State

            data object Dark :
                State

            override fun deepCopy():
                State =
                this
        }

        /**
         * Semantic input region.
         *
         * Toggle is a singleton for this patient zero. Control is sealed rather than enum-based so
         * later Families are not forced into finite payload-free Controls.
         */
        sealed interface Control :
            VexBehaviorFamily.Control<
                ThemeModeFamily
            > {
            data object Toggle :
                Control
        }

        /**
         * Behavior is deliberately exposed as a Family member in the POC.
         *
         * This closes the semantic circuit at one discoverable root:
         *
         *     ThemeModeFamily
         *       State
         *       Control
         *       Behavior
         *
         * The next Origin-Walk refinement may pressure what Behavior should expose toward UI, but
         * the caller should not need a separate Control resolver or Family.Act vocabulary.
         */
        val Behavior =
            vexBehaviorV2(
                family =
                    this,
                behaviorRef =
                    "example.theme-mode.v2",
            ) {
                // Act: while no override is stored, Toggle means attempt durable Dark and remain
                // immediately in current truth until Save succeeds.
                state<State.NotSet> {
                    on(
                        control =
                            Control.Toggle,
                        effect =
                            begin(
                                mode =
                                    VexBehaviorV2BeginMode.Sequenced,
                            ) {
                                save(
                                    State.Dark,
                                ).results {
                                    success {
                                        admit<
                                            State.Dark
                                        >()
                                    }

                                    failure {
                                    }
                                }
                            },
                    ).stay()
                }

                // Act: current Android ThemeMode has no persistence/provider Effect. While Light is
                // current, Toggle immediately admits Dark without inventing a no-op Effect.
                state<State.Light> {
                    on(
                        control =
                            Control.Toggle,
                    ).admit<
                        State.Dark
                    >()
                }

                // The same Control in another State is a different behavioral scenario. Again, no
                // Effect is earned merely because semantic State changes.
                state<State.Dark> {
                    on(
                        control =
                            Control.Toggle,
                    ).admit<
                        State.Light
                    >()
                }
            }
    }

    @Test
    fun onStatePlusControlIsTheActWithoutAnotherFamilyVocabulary() {
        val act =
            ThemeModeFamily.Behavior
                .resolve(
                    current =
                        ThemeModeFamily.State.Light,
                    control =
                        ThemeModeFamily.Control.Toggle,
                )

        // The removal test: everything needed to understand the Act survives without
        // ThemeModeFamily.Act.SelectDark.
        assertSame(
            ThemeModeFamily.Control.Toggle,
            act?.control,
        )
        assertNull(
            act?.effect,
        )
        assertEquals(
            VexBehaviorV2StateDispositionKind.Admit,
            act?.immediateDisposition
                ?.kind,
        )
        assertEquals(
            ThemeModeFamily.State.Dark::class,
            act?.immediateDisposition
                ?.stateKind,
        )
    }

    @Test
    fun sameControlCanMeanDifferentActsBecauseCurrentStateOwnsTheScenario() {
        val fromLight =
            ThemeModeFamily.Behavior
                .resolve(
                    current =
                        ThemeModeFamily.State.Light,
                    control =
                        ThemeModeFamily.Control.Toggle,
                )

        val fromDark =
            ThemeModeFamily.Behavior
                .resolve(
                    current =
                        ThemeModeFamily.State.Dark,
                    control =
                        ThemeModeFamily.Control.Toggle,
                )

        assertNull(
            fromLight?.effect,
        )
        assertNull(
            fromDark?.effect,
        )
        assertEquals(
            ThemeModeFamily.State.Dark::class,
            fromLight?.immediateDisposition
                ?.stateKind,
        )
        assertEquals(
            ThemeModeFamily.State.Light::class,
            fromDark?.immediateDisposition
                ?.stateKind,
        )
    }

    @Test
    fun durabilityGatedActCanAdmitStateLaterAfterSaveSuccess() {
        val act =
            ThemeModeFamily.Behavior
                .resolve(
                    current =
                        ThemeModeFamily.State.NotSet,
                    control =
                        ThemeModeFamily.Control.Toggle,
                )

        assertSame(
            ThemeModeFamily.State.Dark,
            act?.effect
                ?.save
                ?.state,
        )
        assertEquals(
            VexBehaviorV2StateDispositionKind.Stay,
            act?.immediateDisposition
                ?.kind,
        )
        assertEquals(
            ThemeModeFamily.State.Dark::class,
            act?.effect
                ?.save
                ?.results
                ?.success
                ?.admittedStateKind,
        )
        assertNull(
            act?.effect
                ?.save
                ?.results
                ?.failure
                ?.admittedStateKind,
        )
    }

    @Test
    fun themeModeStateCanAlsoBeRelayVexStateWithoutInventingAnotherStateNoun() {
        val copied:
            ThemeModeFamily.State =
            ThemeModeFamily.State.Dark
                .deepCopy()

        assertSame(
            ThemeModeFamily.State.Dark,
            copied,
        )

        assertTrue(
            ThemeModeFamily.Behavior
                .describe()
                .contains(
                    "Light --on(Toggle)",
                ),
        )
    }

    @Test(
        expected =
            IllegalArgumentException::class,
    )
    fun saveResultsStillRequireExplicitSuccessAndFailure() {
        // This negative proof exists because removing the Act value must not weaken the already-earned
        // two-scope Save contract.
        vexBehaviorV2<
            ThemeModeFamily,
            ThemeModeFamily.State,
            ThemeModeFamily.Control
        >(
            family =
                ThemeModeFamily,
            behaviorRef =
                "example.theme-mode.v2.incomplete-results",
        ) {
            state<ThemeModeFamily.State.Light> {
                on(
                    control =
                        ThemeModeFamily.Control.Toggle,
                    effect =
                        begin(
                            mode =
                                VexBehaviorV2BeginMode.Replaceable,
                        ) {
                            save(
                                ThemeModeFamily.State.Dark,
                            ).results {
                                success {
                                }
                            }
                        },
                ).admit<
                    ThemeModeFamily.State.Dark
                >()
            }

            state<ThemeModeFamily.State.Dark>()
        }
    }
}

// [VXG RealForever]
