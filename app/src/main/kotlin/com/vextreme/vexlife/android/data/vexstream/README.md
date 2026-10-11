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
   confirmation, explicit desktop Allow/Deny, remembered encrypted token, route rediscovery, and
   two-sided forget/revoke.

The next unconsumed lived proofs are deliberately smaller:

1. Pair with the same-Wi-Fi product path without ADB.
2. Download one real song.
3. Stop or disconnect the desktop and play that song as LOCAL_ONLY.
4. Restart VexLife and prove the verified offline copy is restored.
5. Change/reacquire the desktop LAN route and prove certificate-identity rediscovery without
   re-approval.

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
  -> dynamic tracks/albums/artists
  -> feature-local album/artist drill-down state
  -> MediaController commands

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

