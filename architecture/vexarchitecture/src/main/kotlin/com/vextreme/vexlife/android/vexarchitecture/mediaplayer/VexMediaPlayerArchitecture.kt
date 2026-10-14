package com.vextreme.vexlife.android.vexarchitecture.mediaplayer

import com.vextreme.vexlife.android.vexarchitecture.VexArchitectureContract

/**
 * Reusable architecture contract for the familiar human idea of a media player.
 *
 * The package tree is organized first by the part of the media-player experience a person would
 * recognize: playback/ and queue/. Technical roles such as Contract and Provider live as files
 * inside those human-readable areas.
 */
interface VexMediaPlayerArchitecture :
    VexArchitectureContract

// [VXG RealForever]
