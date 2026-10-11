package com.vextreme.vexlife.android.runtime

import com.vextreme.vexlife.android.elements.ActionKey
import com.vextreme.vexlife.android.elements.UIElement
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class AppActionRouterTest {
    private object Source : UIElement(
        elementId = "element.test.source",
        kind = "Test",
    )

    @Test
    fun exactlyOneRegisteredOwnerReceivesTheAction() {
        val action = ActionKey("action.test.one")
        var calls = 0
        val router = AppActionRouter(
            registrations = listOf(
                appAction(
                    action = action,
                    ownerRef = "screen.one",
                ) {
                    calls += 1
                },
            ),
        )

        router.dispatch(
            AppActionRequest(
                action = action,
                source = Source,
            ),
        )

        assertEquals(1, calls)
    }

    @Test
    fun duplicateActionOwnershipIsRejectedAtComposition() {
        val action = ActionKey("action.test.duplicate")

        try {
            AppActionRouter(
                registrations = listOf(
                    appAction(action, "screen.one") {},
                    appAction(action, "screen.two") {},
                ),
            )
            fail("duplicate action ownership should fail")
        } catch (error: IllegalStateException) {
            check(error.message.orEmpty().contains("screen.one"))
            check(error.message.orEmpty().contains("screen.two"))
        }
    }

    @Test
    fun unregisteredActionIsNotSilentlyBroadcastOrIgnored() {
        val router = AppActionRouter(emptyList())

        try {
            router.dispatch(
                AppActionRequest(
                    action = ActionKey("action.test.missing"),
                    source = Source,
                ),
            )
            fail("missing action owner should fail")
        } catch (error: IllegalStateException) {
            check(error.message.orEmpty().contains("action.test.missing"))
        }
    }
}
