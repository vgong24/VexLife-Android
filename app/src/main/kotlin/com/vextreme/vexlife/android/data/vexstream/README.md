# VexStream mobile consumer seam

[VXG RealForever]

## Question

How can VexLife play a VexStream library on the phone, optionally keep verified songs for offline
playback, and cross the computer/phone permission boundary without exposing the desktop application
as a general remote-admin server?

This folder owns the Android consumer projection only. It does not own VexStream desktop source,
Home/Safety pairing semantics, network listener exposure, or Media3 playback.

## Source observations that earned this seam

The supplied VexStream Music 2.1.0 source establishes:

- the desktop HTTP server binds to 127.0.0.1;
- /media/<track> serves exact MP3 bytes with HTTP Range support;
- Track currently includes desktop path, filename, and source information;
- the current local track.id is derived from the normalized desktop file path;
- the browser owns the current queue/player state.

The live canonical VexStream GitHub lane is newer than the supplied package: draft PR #1 currently
contains the in-flight 2.2.0 Explore refinement. Android therefore consumes the source observations
above without creating a second VexStream desktop writer.

The matching desktop producer work is source-addressable in `vgong24/VexstreamMusic` on
`VXG-101126-vexstream-mobile-bridge`; its canonical cross-repository contract is
`docs/architecture/MOBILE-BRIDGE-V1.md`. VexStream PR #1 remains the separate 2.2 Explore acceptance
lane rather than absorbing this bridge work.

## Permanent distinctions

    HUB_TRACK_REF != PORTABLE_MEDIA_IDENTITY
    HUB_LIBRARY != PHONE_OFFLINE_LIBRARY
    STREAM_PERMISSION != COPY_PERMISSION
    REACHABILITY != AUTHENTICATION != AUTHORIZATION
    PAIRED_DEVICE != ACTIVE_CAPABILITY_LEASE
    DOWNLOADED_COPY != CANONICAL_DESKTOP_LIBRARY_OBJECT
    PLAYBACK_OWNER != CATALOG_REPOSITORY

A Hub track ref addresses the current desktop catalog object. mediaSha256 binds the exact media
bytes and is the portable identity used to preserve a verified phone copy if the desktop path/ref
later changes.

## Producer projection Android needs

Do not send the existing desktop Track JSON across the bridge verbatim.

The VexStream-owned bridge should eventually provide a sanitized projection equivalent to:

    hubRef
    sourceVersion
    catalogRevision

    track
      hubTrackRef
      mediaSha256
      mediaByteLength
      title
      artist
      album
      genre
      year
      duration
      playbackStart
      playbackEnd

No desktop path, source-folder path, private listener address, credential, raw Home/model endpoint, or
general VexStream administrative API belongs in that projection.

The offline requirement makes mediaSha256 + mediaByteLength material producer fields: Android must be
able to verify the completed copy before representing it as downloaded.

## Android 17 local-network boundary

VexLife currently targets SDK 37. Android 17 therefore blocks ordinary local-network traffic by
default unless the app uses an admitted system-mediated route or the user grants broad
ACCESS_LOCAL_NETWORK permission.

For the first VexStream pairing path, prefer the Android NsdManager system service picker
(DiscoveryRequest.FLAG_SHOW_PICKER) when the final VexStream service type/advertisement contract is
available. The system lets the human select one discovered service; addresses obtained from that
selection can be contacted without granting VexLife blanket LAN discovery/access.

That system selection proves only the Android reachability allowance for the chosen service:

    SYSTEM_SELECTED_SERVICE != VEXSTREAM_PAIRING
    SYSTEM_SELECTED_SERVICE != AUTHENTICATION
    SYSTEM_SELECTED_SERVICE != CAPABILITY_LEASE

Home/Safety pairing and authorization still gate catalog/media effects above that transport.

If a later feature genuinely requires broad persistent LAN discovery rather than one selected Hub,
that expansion can deliberately request ACCESS_LOCAL_NETWORK instead of silently making it a
prerequisite now.

## Capability shape

Android currently names only consumer-level capability classes:

    CATALOG_READ
    MEDIA_STREAM
    MEDIA_COPY

Those are not accepted security capability-ref strings. Current Vextreme-SDK Home Access/Security
source owns pairing/authentication/authorization/capability-lease meaning, and currently admits a
different canonical capability family. Real VexStream capability refs must be added/accepted by that
rightful security owner before Android claims authorization.

This separation lets streaming and offline copying be granted independently.

## Phone storage

A real Android media store should default to app-private storage. The phone therefore does not need
broad shared-storage ownership merely to keep VexStream offline copies.

The local store owns:

    expected media digest/length
    download progress
    atomic completion
    verified local media ref
    remove-local-copy

Local paths remain implementation detail. Higher layers receive an opaque localMediaRef.

## Playback

This repository answers availability:

    HUB_ONLY
    HUB_AND_LOCAL
    LOCAL_ONLY
    UNAVAILABLE

It does not own audio playback. A later Android Media3 owner should prefer a verified local copy when
appropriate and otherwise stream from the authorized Hub bridge. Playback must continue to work for
LOCAL_ONLY tracks with the Hub unreachable.

## Current threshold map

Already source-formed and/or observed:

1. Android catalog/offline state + media-identity merge semantics.
2. App-private verified media store + durable offline index.
3. Journey-aware VexStream screen and feature-owned rendering.
4. Media3 player/session ownership with phone-local-first playback resolution.
5. VexStream-owned sanitized catalog + range-media producer.
6. Physical Samsung proof through the development bridge: real desktop library -> phone playback.
7. Same-Wi-Fi product pairing candidate: Android service picker/NSD, pinned TLS, matching-code
   confirmation, explicit desktop Allow/Deny, remembered encrypted token, certificate-identity re-selection after route change, and
   two-sided forget/revoke.

The next unconsumed lived proofs are deliberately smaller:

1. Pair with the same-Wi-Fi product path without ADB.
2. Download one real song.
3. Stop or disconnect the desktop and play that song as LOCAL_ONLY.
4. Restart VexLife and prove the verified offline copy is restored.
5. Change/reacquire the desktop LAN route, use Find VexStream to re-select the same certificate identity, and prove no new desktop approval is required.

Away-from-home streaming remains a later VexHome/Home-Safety authority problem rather than a reason
to widen this local bridge.

<!-- [VXG RealForever][VEXLIFE-ANDROID][VEXSTREAM][MOBILE-CONSUMER] -->

## First physical-device bridge proof (preserved recovery path)

The original development transport expects the matching VexStream desktop branch:

```text
vgong24/VexstreamMusic
VXG-101126-vexstream-mobile-bridge
desktop loopback = 127.0.0.1:18765
```

With that desktop candidate running, map the physical Android device's loopback port:

```text
adb reverse tcp:18765 tcp:18765
```

Then run this Android branch normally and open:

```text
Home
-> VexStream Music
```

The screen automatically attempts a catalog refresh. The expected progression is:

```text
Desktop library connected
-> sanitized track rows
-> Play uses Media3 on the phone
-> Download verifies exact bytes into app-private storage
-> remove desktop transport / leave Hub unavailable
-> downloaded row becomes LOCAL_ONLY
-> Play resolves the verified phone copy
-> app restart restores the verified offline index
```

The debug manifest permits cleartext loopback HTTP only for this preserved developer/recovery
transport. The normal same-Wi-Fi path below uses pinned TLS and does not depend on ADB.

## Current same-Wi-Fi product pairing candidate

The lived ADB-reverse bridge proved the catalog/media relationship on a physical Samsung phone. It is
preserved as developer/recovery scaffolding. The normal local connection candidate now removes ADB
from the human path:

```text
Windows VexStream
  -> Connect a phone
  -> advertises _vexstream._tcp for a bounded pairing window
  -> TLS certificate fingerprint + six-digit human confirmation code

VexLife
  -> Find VexStream
  -> Android nearby-service selection
  -> pins the advertised certificate before sending pairing data
  -> shows the same six-digit confirmation code
  -> requests access

Windows VexStream
  -> human Allow / Deny

Allow
  -> random bearer token
  -> desktop persists token hash only
  -> Android encrypts the token with Android Keystore
  -> catalog/media use pinned TLS + bearer
  -> remembered same-Wi-Fi reconnect
```

For Android 17+ the consumer uses the system NSD service picker rather than requesting blanket local
network permission. Older Android releases use the existing NSD service-discovery API.

A remembered VexStream is bound to the desktop certificate identity, not a temporary DHCP address. If
the desktop route changes, Find VexStream can rediscover the same certificate and update host/port
without requiring another approval.

This local pairing candidate deliberately does not claim canonical VexHome/Home-Safety authority:

```text
LOCAL_VEXSTREAM_PAIRING
!= HOME_MEMBERSHIP
!= CANONICAL_AUTHENTICATION
!= CAPABILITY_LEASE
!= REMOTE_ACCESS_AUTHORITY
```

Away-from-home streaming still belongs behind the rightful Home/Safety route.

## Current VexLife foundation alignment

The feature is currentized onto VexLife-Android main
`1b5f1c8cb06a479f2768ce1f8f8557b61694f15d`, including the merged ScreenCrawler/Journey evidence
foundation.

The current Android shape follows the repository's existing feature pattern rather than introducing a
second navigation/runtime system:

```text
one AppScreenBlueprint
  -> stable connection actions
  -> swappable library lenses
       Songs
       Albums
       Artists
       Playlists
       Downloads
  -> one registered Queue sheet

custom feature renderer
  -> dynamic tracks/albums/artists/playlists
  -> NavigationTrail-owned collection drill-down
  -> MediaController commands

VexRuntime Back chain
  -> Queue sheet first
  -> current collection detail next
  -> VexStream Screen navigation after local depth reaches root

MediaSessionService
  -> playback lifetime
  -> current queue
  -> shuffle/repeat
  -> previous/next
```

Dynamic music objects do not become one registered blueprint element per song. The blueprint names
durable semantic surfaces; repository data remains data.

The semantic journey uses the current typed `Journeys.vexStream...` authoring surface and exercises
connection, every registered library lens, Queue open/close, and forget.

Blocking HTTPS/media/file work stays inside the repository/media boundary on `Dispatchers.IO`.
Compose and Screen action scopes request semantic operations; they do not perform
`HttpURLConnection` work on the Android UI dispatcher.

### Playlist ownership

Desktop playlists are currently projected read-only into the sanitized mobile catalog:

```text
desktop playlist writer
  -> playlistRef + name + ordered current track refs
  -> Android browse/play/shuffle

Android
  -> no silent desktop playlist mutations
```

This is intentionally one-way for the current streaming milestone. A later phone-local playlist
writer can remain local until an explicit sync action/contract is admitted; two-way conflict rules
are not invented here.

## Vex element/runtime realization

The Android renderer follows the same semantic/Compose split demonstrated by Debug -> Log History and
User Profile:

```text
AppScreenBlueprint
  -> registered container/action identity
  -> VexContainer / VexButton realization
  -> VexRuntime validation + semantic interaction log
  -> VexLogger
  -> LogHistoryPersistenceQueue
```

Compose remains the Android renderer. The important boundary is that durable product controls do not
bypass the registered element/runtime path merely because the Screen has a custom renderer.

VexStream therefore keeps:
- connection controls, library lenses, collection actions, selected-track actions, mini-player
  actions, Queue actions, and modal identity as registered Screen elements;
- dynamic songs/albums/artists/playlists as repository data rather than synthesizing one static
  blueprint node per media row;
- track-row selection and collection drill-down as local presentation state;
- MediaSessionService as playback lifetime/queue owner;
- blocking transport/media/file work on `Dispatchers.IO`.

`ElementIntent.LocalEffect` exists for a narrow custom-renderer case: the registered control must
still enter VexRuntime for validation/logging, while the concrete effect depends on renderer-scoped
context such as the current MediaController or selected dynamic row. Stable construction-time app
effects continue to use Invoke/AppActionRegistrations.

This keeps:

```text
CUSTOM_COMPOSE_RENDERER != BYPASS_SEMANTIC_RUNTIME
DYNAMIC_CATALOG_DATA != STATIC_BLUEPRINT_TOPOLOGY
LOCAL_EFFECT != GENERIC_SERVICE_LOCATOR
```

### Lived same-Wi-Fi checkpoint

Observed on physical Samsung SM-S908U with the VexStream 3.0 Windows producer:

```text
Find VexStream
-> same-Wi-Fi connection succeeds
-> real desktop catalog appears
-> selected desktop-backed track plays
-> MediaSession mini-player projects current playback
```

This is product-path evidence, not the earlier ADB-reverse developer bridge.

Still owed as separate lived predicates:
- verified Download completion;
- desktop unavailable -> LOCAL_ONLY playback;
- Android restart -> offline restoration;
- real desktop playlist projection visible/playable on the phone.

### Current mobile browse refinement

The real library size showed that phone browsing needs a compact first-class search surface. One
registered `SearchQuery` field now filters Songs / Albums / Artists / Playlists / Downloads while
typing remains local presentation state, matching the User Profile draft-input pattern rather than
creating one semantic log event per keystroke.

Long/duplicated title/artist/album presentation is compacted for the phone, while stable playback
controls remain registered Vex actions and dynamic catalog rows remain repository data.


### Playback lifetime + perceivable mobile player refinement

A006 proved the prior exact head through the Mac Android toolchain and the complete requested
assemble/source-alignment/architecture/app-unit suite. The next lived phone run then exposed a
different product concern: leaving the VexStream Screen and returning could interrupt playback, while
the text-button mini-player did not make timeline state easy to perceive.

Source inspection showed that the player itself already had the right Android owner:

```text
VexStreamPlaybackService
  -> MediaSession
  -> ExoPlayer
  -> queue / playback lifetime
```

The UI-side `MediaController`, however, was created and released with VexStream Screen composition.
The current correction removes that coupling without moving playback ownership into Compose:

```text
VexStreamPlaybackService
  owns Player + MediaSession + queue

VexStreamPlaybackViewModel
  retains only the MediaController client across ordinary VexLife Screen navigation

VexStream Screen
  observes playback state
  -> registered Vex controls
  -> VexRuntime / logging
  -> renderer-local MediaController effect
```

The correction is still owed lived-device verification; static source shape is not a substitute for
proving the reported navigation behavior on the phone.

The mobile player also now takes layout cues from the desktop VexStream experience without copying
desktop UI architecture: selected tracks have a perceivable selected surface, high-frequency
track/player actions use registered semantic icon controls, and the mini-player projects
title/artist, queue position, elapsed/duration timeline, and compact transport/shuffle/queue controls.
The semantic element identities remain unchanged, so custom Compose realization still enters
VexRuntime before each local playback effect.

```text
DESKTOP_PLACEMENT_CUE != DESKTOP_UI_PORT
MEDIASESSION_SERVICE = PLAYBACK_OWNER
VIEWMODEL_MEDIA_CONTROLLER = RETAINED_UI_CLIENT
ICON_REALIZATION != BYPASS_SEMANTIC_RUNTIME
LIVED_NAVIGATION_PROOF = STILL_REQUIRED
```


### Library-first Android UX scaffold

Lived Android review showed that connection/setup chrome and foundation-era labels occupied more
space than the music itself. The current realization therefore treats VexStream as a music
destination inside VexLife.

The shared AppToolbar now renders Back, the Vex mark, the current Screen title, and optional
Screen-owned registered toolbar actions. VexStream contributes Search and Manage Connection without
moving their ownership into the shell.

When paired, the VexStream body is library-first. Category controls are list content that can scroll
away, global Play All and Shuffle All use registered floating-action realization, compact track rows
reserve a leading artwork slot, and the mini-player projects actual play/pause and shuffle state.
When unpaired, connection setup remains prominent because connection is the current task.

Search is contextual presentation rather than a permanently mounted large field. SearchQuery remains
the same registered editable semantic identity and is realized only while the Search sheet is open.

The track list adds an artwork slot without inventing artwork data. The current VexStream catalog
contract carries music metadata and media identity but no admitted cover-art reference or bytes, so
Android uses a music placeholder until that producer/consumer contract is deliberately extended.

YOUTUBE_MUSIC_LAYOUT_CUE != YOUTUBE_MUSIC_PRODUCT_COPY
ARTWORK_SLOT != FABRICATED_ARTWORK_SOURCE
SCREEN_TOOLBAR_ACTION != SHELL_FEATURE_OWNERSHIP
VISIBLE_SELECTED_STATE != NEW_PLAYBACK_TRUTH
LIBRARY_DENSITY != SEMANTIC_IDENTITY_LOSS


### Off-home route successor

The same-Wi-Fi pairing remains the bootstrap and preferred direct route. The remote successor does not
replace MediaSession, the VexStream repository, or verified offline storage.

```text
VexStreamNetworkDataSource / repository request
  -> remembered paired connection

  try pinned LAN HTTPS first

  if LAN transport is unreachable
  AND remote pairing material is present
    -> signed + end-to-end encrypted relay request
    -> desktop outbound VexStream gateway
    -> existing desktop mobile HTTPS surface

  if no network route
    -> verified local media remains independently usable
```

The long-lived Android device signing private key is generated in Android Keystore. Local pairing
sends only its P-256 public key. Existing v1 pairings opportunistically call the bearer-authorized
remote-upgrade endpoint while the desktop is directly reachable, so remote enrollment does not
require a mandatory Forget/re-pair cycle.

Remote request confidentiality is separate from relay TLS. Android creates an ephemeral P-256 ECDH
key for each request, derives an AES-GCM request key with the desktop's paired remote-gateway public
key, and encrypts the existing VexStream bearer plus the bounded catalog/media request. The relay can
verify the paired device signature but cannot decrypt that inner payload.

Media fallback is Range-based and bounded rather than materializing an entire song in one relay
response.

The connection store accepts the previous local-pairing schema for compatibility; newly saved
connections use v2 and may additionally retain:

```text
remoteHomeRef
remoteGatewayPublicKey
remoteRelayBaseUrl
```

Those fields are reachability inputs, not a claim that Android minted canonical Home/Safety
membership or authority.

The remote route becomes executable only when a real HTTPS relay URL has been deployed and supplied
by the desktop during local pairing/upgrade. Source formation alone does not claim off-home lived
reachability.

```text
LAN_FIRST
REMOTE_SECOND
VERIFIED_LOCAL_COPY_ALWAYS_INDEPENDENT
PAIRED_DEVICE_SIGNATURE != GENERAL_HOME_AUTHORITY
REMOTE_VEXSTREAM_READ != GENERAL_PC_REMOTE_CONTROL
```
