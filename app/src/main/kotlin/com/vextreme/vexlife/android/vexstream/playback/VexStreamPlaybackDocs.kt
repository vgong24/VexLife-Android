package com.vextreme.vexlife.android.vexstream.playback

/**
 * # VexStream Playback
 *
 * VexStream music relates Vex semantic owners to one external Player Partner Source.
 *
 *     VexStreamPlayerPartner
 *       Source       one external music/playback origin
 *       Signal       outside statements
 *       Connection   live access paths to that Source
 *
 * Current Media3 realization:
 *
 *     Media3PlayerConnection
 *         observation path from ExoPlayer
 *
 *     Media3ControllerConnection
 *         request/progress path through MediaController
 *
 * Player / MediaSession / MediaController are realization/access mechanisms around the same Source;
 * their Android class boundaries do not manufacture separate product owners.
 *
 * ## Inbound
 *
 *     external Source
 *       -> Media3PlayerConnection
 *       -> Conversion
 *       -> Signal / Observation
 *       -> Receiver
 *       -> VexStreamPlayerStore.Snapshot
 *       -> VexExecution observation
 *       -> Playback / Queue / Repeat Family
 *
 * Receiver ends at Store. Store owns retained outside truth. Family owns semantic meaning.
 *
 * ## Playback semantic loop
 *
 *     Playback Control
 *       -> VexStreamPlaybackFamily.activate(...)
 *       -> accepted Act
 *       -> optional pending State
 *       -> Effect
 *       -> bound VexStreamPlaybackFamily.Act.Method
 *       -> VexExecution
 *       -> VexStreamPlayerPartner.Connection.Request
 *       -> external Source
 *
 * Provider acknowledgement/outcome returns only through Player observation.
 *
 * Playback owns the feature-specific reconciliation meaning because a stale provider statement
 * must not erase a newer pending Play/Pause intent. [VexStreamPlaybackFamily.observePlayer] enters
 * that decision through VexFamily's protected admitCurrent boundary, so provider reconciliation and
 * ordinary Control activation serialize against the same exact Family CURRENT without a second lock
 * or State owner.
 *
 * ## Provider read facts
 *
 * [VexStreamPlaybackReadState] is a bounded presentation read model composed from Player Store
 * Snapshot plus live [VexStreamPlayerPartner.Connection.Progress]. Full QueueItems remain Queue/Store
 * owned.
 *
 * ## Widget
 *
 * The Playback Widget's [com.vextreme.vexlife.android.widget.vexstream.VexStreamPlaybackWidgetRelay]
 * is an ordinary Compound Relay over bounded Player Store + Playback Family currents. Widget durable
 * SurfaceState / Projection / ActionRef remain presentation mechanics; no peer VexWidget semantic
 * root is required.
 *
 * ## Sustained realization
 *
 * VexStreamPlaybackService owns Android MediaSession/ExoPlayer lifetime. It borrows semantic
 * Playback CURRENT and hosts the observation Connection; Service lifetime is not Playback truth.
 */
@Suppress("unused")
internal class VexStreamPlaybackDocs private constructor()

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
