package com.vextreme.vexlife.android.runtime

import com.vextreme.vexlife.android.elements.ActionKey
import com.vextreme.vexlife.android.elements.ButtonElement
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.invoke
import com.vextreme.vexlife.android.elements.navigateTo
import com.vextreme.vexlife.android.elements.then
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AppActionRegistrationsTest {
    private object TargetScreen : ScreenElement(
        screenId = "screen.test-target",
        titleRes = 1,
    )

    private val first = ActionKey("action.test.first")
    private val second = ActionKey("action.test.second")

    private val invokeSource = ButtonElement(
        elementId = "element.test.invoke",
        labelRes = 2,
        intent = invoke(first),
    )

    private val sequenceSource = ButtonElement(
        elementId = "element.test.sequence",
        labelRes = 3,
        intent = invoke(first) then invoke(second),
    )

    private val navigationSource = ButtonElement(
        elementId = "element.test.navigate",
        labelRes = 4,
        intent = navigateTo(TargetScreen),
    )

    @Test
    fun collectionIsQueryableByActionKeyAndByElementIntent() {
        val registrations = AppActionRegistrations.of(
            appAction(first, "screen.one") {},
            appAction(second, "screen.two") {},
        )

        assertEquals("screen.one", registrations[first]?.ownerRef)
        assertEquals(
            listOf("screen.one"),
            registrations[invokeSource].map { it.ownerRef },
        )
        assertEquals(
            listOf("screen.one", "screen.two"),
            registrations[sequenceSource].map { it.ownerRef },
        )
        assertTrue(registrations[navigationSource].isEmpty())
    }

    @Test
    fun coverageRejectsRegisteredInvokeActionsWithoutOwners() {
        try {
            AppActionRegistrations.Empty.requireCoverage(
                listOf(invokeSource, navigationSource),
            )
            fail("declared Invoke action without an owner should fail coverage")
        } catch (error: IllegalStateException) {
            assertTrue(error.message.orEmpty().contains(first.actionId))
        }
    }

    @Test
    fun exactlyOneRegisteredOwnerReceivesTheAction() {
        var calls = 0
        val registrations = AppActionRegistrations.of(
            appAction(first, "screen.one") {
                calls += 1
            },
        )

        registrations.dispatch(
            AppActionRequest(
                action = first,
                source = invokeSource,
            ),
        )

        assertEquals(1, calls)
    }

    @Test
    fun duplicateActionOwnershipIsRejectedWhenRegistrationsCombine() {
        val one = AppActionRegistrations.of(
            appAction(first, "screen.one") {},
        )
        val two = AppActionRegistrations.of(
            appAction(first, "screen.two") {},
        )

        try {
            AppActionRegistrations.combine(one, two)
            fail("duplicate action ownership should fail")
        } catch (error: IllegalStateException) {
            check(error.message.orEmpty().contains("screen.one"))
            check(error.message.orEmpty().contains("screen.two"))
        }
    }

    @Test
    fun unregisteredActionFailsInsteadOfBecomingABroadcastOrSilentNoOp() {
        val registrations = AppActionRegistrations.Empty

        try {
            registrations.dispatch(
                AppActionRequest(
                    action = first,
                    source = invokeSource,
                ),
            )
            fail("missing action owner should fail")
        } catch (error: IllegalStateException) {
            check(error.message.orEmpty().contains(first.actionId))
        }
    }
}
