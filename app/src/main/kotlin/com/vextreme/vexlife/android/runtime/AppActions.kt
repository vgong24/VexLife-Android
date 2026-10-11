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
    val GallerySample = ActionKey("action.gallery.sample")
    val SaveUserProfile = ActionKey("action.user-profile.save")
    val VexStreamMusicPlaceholder = ActionKey("action.vexstream-music.placeholder")
    val TalkToVexPlaceholder = ActionKey("action.talk-to-vex.placeholder")
}
