package com.vextreme.vexlife.android.vexstream.experience.v2

import com.vextreme.vexlife.android.vexstream.experience.v2.player.VexStreamPlayerArchitecture

/**
 * Human/product front door for the whole VexStream experience.
 *
 * The experience can be read as one ordinary sentence:
 *
 * A listener comes to VexStream because they want to experience the music they know wherever it can
 * be realized. [VexStreamLibrary] tells them WHAT music is known. [VexStreamSources] tells VexStream
 * WHERE playable media can come from. [VexStreamPlayerArchitecture] defines HOW listening is
 * structured. Playback/Queue Cases and Placements explain WHAT IS TRUE / POSSIBLE NOW. Controls name
 * WHAT CAN BE REQUESTED. Player Providers realize those meanings through Media3 or future SDK
 * adapters. UI projections return the observed result to the listener.
 *
 * Question lens:
 *
 * HOME = VexStream
 * WHO = listener / consumer
 * WHY = the whole music experience and why these pieces belong together
 * WHAT = Library
 * WHERE = Sources
 * HOW = Player Architecture
 * NOW / WHEN = Case + Placement
 * WHAT CAN I DO = Control
 * WHO / WHAT REALIZES IT = Provider
 * WHAT DO I SEE = projection
 *
 * "Experience" is therefore intentionally broader than "Architecture": it integrates different
 * rightful owners without becoming their manager. It does not reimplement repositories, source
 * resolution, providers, Media3, downloads, networking, or player lifetime.
 *
 * The package carries the version. The class name stays production-shaped so a future maintainer
 * does not have to treat every V2 type as temporary.
 *
 * Repository authoring language:
 * docs/foundation/FEATURE-EXPERIENCE-LANGUAGE.md
 */
object VexStreamExperience {
    /** Stable semantic identity for the whole VexStream product experience. */
    const val experienceRef: String = "vexstream"

    /**
     * What music VexStream knows about.
     *
     * Library means songs/albums/artists/playlists/catalog identity. It does NOT mean where media
     * bytes are stored and does not replace the existing repository that owns current library data.
     */
    val library: VexStreamLibrary
        get() = VexStreamLibrary

    /**
     * Where playable media may come from.
     *
     * Connection is therefore a state of a remote Source, while "availability" is an answer derived
     * from which Sources can satisfy a piece of media. Neither needs to become a peer architecture.
     */
    val sources: VexStreamSources
        get() = VexStreamSources

    /**
     * How active media-player meaning is organized.
     *
     * PlayerArchitecture describes Playback/Queue semantics; actual SDK execution remains in
     * providers below those capabilities.
     */
    val player: VexStreamPlayerArchitecture
        get() = VexStreamPlayerArchitecture
}

/**
 * Semantic orientation node for music known to VexStream.
 *
 * The existing VexStreamMusicRepository remains the data/state owner. This object exists only so a
 * fresh developer can distinguish "what music is known?" from "where can its media come from?".
 */
object VexStreamLibrary {
    /** Stable diagnostic/orientation identity; not a storage path. */
    const val libraryRef: String = "vexstream.library"
}

/**
 * Semantic orientation node for possible media origins.
 *
 * Current implementations include verified local bytes and the paired VexStream desktop source.
 * Future partner sources can join here without turning Connection or Availability into top-level
 * product subsystems.
 */
object VexStreamSources {
    /** Stable diagnostic/orientation identity for source resolution as a concern. */
    const val sourcesRef: String = "vexstream.sources"
}

// [VXG RealForever]
