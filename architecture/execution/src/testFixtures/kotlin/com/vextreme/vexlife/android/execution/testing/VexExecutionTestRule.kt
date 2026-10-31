package com.vextreme.vexlife.android.execution.testing

import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.rules.ExternalResource

/**
 * Reusable per-test host for feature code that consumes VexExecution.
 *
 * WHY THIS EXISTS:
 * ordinary coroutine tests tend to repeat scheduler/dispatcher/root setup and rely on each author to
 * remember cancellation/join cleanup. One missed cleanup can leave a collector or restart loop alive
 * and make a later test fail for the wrong reason. This rule makes that lifecycle one reusable law.
 *
 * The rule owns only universal execution-test mechanics:
 * - one fresh virtual-time scheduler;
 * - one StandardTestDispatcher;
 * - one VexExecutionRoot;
 * - guaranteed force-stop/join cleanup after the test, even when setup/assertions fail.
 *
 * Observation recovery belongs to the observation call itself, not this root/test host.
 *
 * Feature construction stays visible in the feature test's @Before method. That is intentional:
 * repository/provider/Execution assembly teaches what the feature actually depends on and should not
 * disappear into a generic scenario builder merely to save lines.
 *
 * Typical shape:
 *
 * @get:Rule
 * val vexExecution = VexExecutionTestRule()
 *
 * @Before
 * fun setUp() {
 *     repository = InMemoryThingRepository()
 *     execution = ThingExecution(vexExecution.root, repository)
 * }
 *
 * @Test
 * fun example() = vexExecution.runTest {
 *     execution.doThing()
 *     runCurrent()
 *     // assert the meaningful intermediate/final state
 * }
 *
 * Teardown does NOT "wait until idle" before cancellation. It closes/cancels the Vex execution tree
 * first, then advances only the already-cancelled virtual scheduler so structured/non-cancellable
 * cleanup can finish before join verification returns.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VexExecutionTestRule :
    ExternalResource() {
    lateinit var scheduler: TestCoroutineScheduler
        private set

    lateinit var dispatcher: TestDispatcher
        private set

    lateinit var root: VexExecutionRoot
        private set

    private var initialized = false

    override fun before() {
        scheduler =
            TestCoroutineScheduler()

        dispatcher =
            StandardTestDispatcher(
                scheduler,
            )

        root =
            VexExecutionRoot(
                dispatcher =
                    dispatcher,
            )

        initialized = true
    }

    /**
     * Runs one feature scenario on the same scheduler/root created before the test.
     *
     * The root is also join-stopped in this block's finally so runTest never waits on a legitimate
     * long-lived Vex collector after the assertions complete. [after] remains the outer safety net
     * for failures that happen before this method is entered or during feature @Before/@After code.
     */
    fun runTest(
        block:
            suspend VexExecutionTestScope.() -> Unit,
    ) {
        check(initialized) {
            "VexExecutionTestRule must be installed with @Rule"
        }

        TestScope(
            dispatcher,
        ).runTest {
            val scope =
                VexExecutionTestScope(
                    root = root,
                    testScope = this,
                )

            try {
                scope.block()
            } finally {
                root.joinStopAll(
                    "Vex execution test body finished",
                )
            }
        }
    }

    override fun after() {
        if (!initialized) {
            return
        }

        try {
            /*
             * Cancellation comes first. We never advance an active execution tree until it happens
             * to become idle because that could hide a leaked/restarting job.
             */
            root.forceStopAll(
                "Vex execution test teardown",
            )

            /*
             * The tree is already cancelled. Advancing now only lets cancellation/finally cleanup
             * scheduled on this virtual dispatcher finish deterministically.
             */
            scheduler.advanceUntilIdle()

            runBlocking {
                root.joinStopAll(
                    "Vex execution test teardown",
                )
            }
        } finally {
            initialized = false
        }
    }
}

/**
 * Small virtual-time surface exposed to feature scenario logic.
 *
 * These names intentionally stay close to kotlinx-coroutines-test so developers can transfer their
 * normal coroutine-testing knowledge. The fixture removes setup/cleanup repetition; it does not hide
 * the scheduling boundaries a race test is trying to prove.
 *
 * Scheduling meaning:
 * - runCurrent(): progress all work that is runnable at the current virtual timestamp, then return
 *   without advancing time. This is the preferred "show me the current concurrency boundary" tool.
 * - advanceTimeBy(...): move virtual time only when elapsed time is part of the behavior being proved.
 * - advanceUntilIdle(): drain all scheduled work; useful for completion-oriented tests, but avoid it
 *   when an intermediate race state is the actual claim because it can skip directly to the end.
 *
 * The point is to remove guessed sleeps/time margins from tests. A test should not say "advance 100ms
 * so the coroutine probably started"; it should run currently-runnable work, inspect reality, and
 * advance time only when a real timer/backoff/timeout contract requires it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VexExecutionTestScope internal constructor(
    val root: VexExecutionRoot,
    private val testScope: TestScope,
) {
    val currentTimeMillis: Long
        get() = testScope.testScheduler.currentTime

    fun runCurrent() {
        testScope.runCurrent()
    }

    fun advanceTimeBy(
        delayMillis: Long,
    ) {
        testScope.advanceTimeBy(
            delayMillis,
        )
    }

    fun advanceUntilIdle() {
        testScope.advanceUntilIdle()
    }
}

// [VXG RealForever]
