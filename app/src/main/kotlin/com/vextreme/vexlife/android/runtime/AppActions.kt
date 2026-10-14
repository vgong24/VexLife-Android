package com.vextreme.vexlife.android.runtime

import com.vextreme.vexlife.android.elements.ActionKey

/**
 * Stable semantic action keys only.
 *
 * Effect ownership does NOT live here. Each Screen keeps its AppActionRegistration beside the
 * feature source, and AppActionRegistrations enforces one owner per key when app composition combines
 * those registration groups.
 */
object AppActions {
    val ToggleDarkMode = ActionKey("action.theme.dark-mode.toggle")
    val ClearLogHistory = ActionKey("action.log-history.clear")
    val StartSyntheticEvents = ActionKey("action.events.synthetic.start")
    val StopSyntheticEvents = ActionKey("action.events.synthetic.stop")
    val EmitSyntheticEvent = ActionKey("action.events.synthetic.emit-next")
    val EmitSyntheticEventBurst = ActionKey("action.events.synthetic.emit-burst")
    val InsertHistoricalSyntheticEvent = ActionKey("action.events.synthetic.insert-historical")
    val RemoveOldestSyntheticEvent = ActionKey("action.events.synthetic.remove-oldest")
    val ResetEventsDebugMenu = ActionKey("action.events.debug-menu.reset")
    val UseRotationDebugMenuAlerts = ActionKey("action.events.debug-menu.alerts.use-rotation")
    val UseRandomDebugMenuAlerts = ActionKey("action.events.debug-menu.alerts.use-random")
    /**
     * Debug-only Events action: prepare one next alert case.
     *
     * The stable action ID keeps the concrete AlertCase vocabulary; this
     * is not a shared alert-case framework action.
     */
    val NextDebugMenuAlertCase =
        ActionKey(
            "action.events.debug-menu.alerts.next-case",
        )
    val GallerySample = ActionKey("action.gallery.sample")
    val SaveUserProfile = ActionKey("action.user-profile.save")
    val FindNearbyVexStream = ActionKey("action.vexstream-music.connection.find")
    val ReconnectVexStream = ActionKey("action.vexstream-music.connection.reconnect")
    val ConfirmVexStreamPairingCode = ActionKey("action.vexstream-music.connection.confirm-code")
    val ForgetPairedVexStream = ActionKey("action.vexstream-music.connection.forget")
    val RefreshVexStreamCatalog = ActionKey("action.vexstream-music.catalog.refresh")
    val TalkToVexPlaceholder = ActionKey("action.talk-to-vex.placeholder")
}
