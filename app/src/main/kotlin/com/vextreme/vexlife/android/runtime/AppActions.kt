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
    val GallerySample = ActionKey("action.gallery.sample")
    val SaveUserProfile = ActionKey("action.user-profile.save")
    val FindNearbyVexStream = ActionKey("action.vexstream-music.connection.find")
    val ConfirmVexStreamPairingCode = ActionKey("action.vexstream-music.connection.confirm-code")
    val ForgetPairedVexStream = ActionKey("action.vexstream-music.connection.forget")
    val RefreshVexStreamCatalog = ActionKey("action.vexstream-music.catalog.refresh")
    val TalkToVexPlaceholder = ActionKey("action.talk-to-vex.placeholder")
}
