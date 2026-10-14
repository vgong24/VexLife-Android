package com.vextreme.vexlife.android.vexarchitecture.mediaplayer.architecture

import com.vextreme.vexlife.android.vexarchitecture.VexArchitectureContract

/**
 * Reusable architecture contract for a feature-specific media player.
 *
 * This package answers HOW media-player behavior is structurally understood. Capability contracts
 * live beneath playback/ and queue/ so a human browsing the tree can find the concern before opening
 * a file.
 *
 * Runtime realization does not live here. See the sibling mediaplayer/provider package.
 */
interface VexMediaPlayerArchitecture :
    VexArchitectureContract

// [VXG RealForever]
