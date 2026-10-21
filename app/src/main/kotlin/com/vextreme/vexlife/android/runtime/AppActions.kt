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
    val ResetSyntheticEventLab = ActionKey("action.events.synthetic.reset-lab")
    val UseRotationAttentionLab = ActionKey("action.events.attention-lab.use-rotation")
    val UseRandomAttentionLab = ActionKey("action.events.attention-lab.use-random")
    val NextAttentionLabScenario = ActionKey("action.events.attention-lab.next-scenario")
    val GallerySample = ActionKey("action.gallery.sample")
    val SaveUserProfile = ActionKey("action.user-profile.save")
    val FindNearbyVexStream = ActionKey("action.vexstream-music.connection.find")
    val ReconnectVexStream = ActionKey("action.vexstream-music.connection.reconnect")
    val ConfirmVexStreamPairingCode = ActionKey("action.vexstream-music.connection.confirm-code")
    val ForgetPairedVexStream = ActionKey("action.vexstream-music.connection.forget")
    val RefreshVexStreamCatalog = ActionKey("action.vexstream-music.catalog.refresh")
    val RefreshFamilyConversation = ActionKey("action.conversation.family.refresh")
    val TalkToVexPlaceholder = ActionKey("action.talk-to-vex.placeholder")
}
