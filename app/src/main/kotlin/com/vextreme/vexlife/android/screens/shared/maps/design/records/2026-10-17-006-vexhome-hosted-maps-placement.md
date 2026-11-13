# [VexMaps Complete] record — Place connected Maps capability in VexHome

[VXG RealForever]

Feature ref: feature.vexmaps  
Recorded at: **2026-10-17**  
Record ref: design.feature.vexmaps.2026-10-17.006  
Tags: **[VexMaps Complete] [VexHome] [Hosted Capability] [Placement] [Self Hosting]**

## Pressure

VMC-01 proved that Android's system Geocoder can be integrated correctly while still being insufficient for ordinary partial/business discovery.

A direct Stadia autocomplete candidate then proved several useful things without ever making a live provider request:

~~~text
provider usefulness needs separate live evidence
API secrets must not live in Android source/binary
provider terms affect evidence retention
provider version semantics must be checked independently
~~~

Before the protected live effect was executed, the larger product question changed:

> Why should the phone own a third-party Maps API relationship at all when VexHome is intended to be the person's persistent computer/home runtime?

The same Home direction is already pressured by music, personal conversation/runtime context and future user-owned services.

## Decision

The preferred connected VexMaps topology is:

~~~text
VexLife Android
    VexMaps experience
    request/result currentness
    device location
    active navigation
    local/system fallback
        |
        | semantic Maps capability
        v
VexHome
    personal capability host
    deployment choice
    large indexes / provider engines
    credentials when external providers are used
        |
        +--> Photon / OpenStreetMap search candidate
        +--> future route engine
        +--> optional external-provider fallback
~~~

Permanent distinctions:

~~~text
ANDROID EXPERIENCE != HOME HOSTED CAPABILITY
HOME CAPABILITY != ONE ENGINE
CAPABILITY CONTRACT != PROCESS BOUNDARY
CAPABILITY CONTRACT != REPOSITORY BOUNDARY
SELF HOSTED != SAME PHYSICAL MACHINE
MANAGED HOSTING != DIFFERENT PRODUCT MEANING
~~~

## Why this belongs in VexHome

The Home can own costs and resources the phone should not:

~~~text
large search indexes
routing graphs
periodic data refresh
long-lived services
provider API credentials
background preparation
storage-heavy personal capability data
~~~

The phone should keep things that must remain truthful while moving:

~~~text
GPS / sensor observation
permission state
active navigation session
rendered experience
offline continuation
request/result currentness
~~~

This lets the Home answer a semantic question while keeping engine/provider mechanics replaceable.

## One host, modular capabilities

The first implementation should prefer one understandable Home composition rather than premature microservices:

~~~text
VexHome
    Core / identity / lifecycle

    capability: Stream
    capability: Maps
    capability: Conversation
    future capabilities...
~~~

A capability may later be realized:

~~~text
in-process
sibling process/container
another owned machine
human-selected cloud VM
optional Vextreme-managed host
~~~

without changing the consumer contract.

"Separate and combined" is therefore a deployment choice, not two different architectures.

## Repository placement

Do **not** create a VexMaps repository merely because VexMaps has a name.

Current rightful sources remain:

~~~text
VexLife
    owns universal/Home capability identity, host lifecycle and cross-platform contract

VexLife-Android
    owns Android VexMaps experience, native currentness, location/navigation and client adapter

engine projects
    Photon / future route engine remain external dependencies unless an actual fork/product earns custody
~~~

A separate Maps host repository is earned only if Maps later has a truly independent release, deployment, contributor, licensing or lifecycle boundary that cannot remain understandable as a Home capability module.

Good modularity should make that future extraction boring rather than necessary now.

## Existing VEXHOME observation is not widened implicitly

Current accepted VexLife capability:

~~~text
capability.vexlife.vexhome.observe
~~~

is a bounded read-only observation of Home identity/runtime/recovery summary.

This record does not redefine it as arbitrary host execution.

The hosted-capability relationship must be added in VexLife through an explicit new/evolved Home capability contract with its own permissions/effect semantics.

## First Maps engine candidate

Photon over OpenStreetMap data is the first search-engine candidate because the current human pressure needs:

~~~text
partial input
business / POI discovery
place / landmark discovery
address discovery
location bias
self hosting
~~~

Photon is not the Maps identity.

The Home contract should make it possible to replace Photon later without changing VexMaps Android's human meaning.

Large index/RAM/storage requirements are deployment facts and must remain visible when deciding whether a Home installs a regional dataset, broader dataset, cloud realization or optional managed service.

## Connectivity / failure law

Connected capability must not erase local truth:

~~~text
Home reachable + Maps search available
    -> connected Home search

Home unavailable + Android/system fallback useful
    -> explicit fallback

network unavailable + prepared local coverage
    -> local/offline search and routing

none available
    -> visible held state + next useful action
~~~

Do not manufacture a generic convergence framework before the concrete Home search consumer exists.

## Immediate consequence for PR #128

The Stadia-specific probe source is removed from the candidate.

Preserved:

~~~text
provider reliability distinction
bounded live-proof laws
public/private fixture discipline
privacy/retention learnings
~~~

Superseded:

~~~text
Stadia as the active VMC-01B candidate
A005 live provider execution
direct Android/provider credential relationship
~~~

No Stadia live request was made.

## Next implementation question

The next source work is not "install Photon in Android."

It is:

~~~text
VexLife
    admit the smallest truthful Home-hosted Maps search capability contract

then

VexHome realization
    prove one local/self-hosted Photon search path

then

VexLife Android
    consume that semantic capability behind existing VexMaps search currentness
~~~

Only after that real second hosted capability exists should shared Home-host abstractions be promoted.

<!-- [VXG RealForever][feature.vexmaps][VEXMAPS-COMPLETE][VEXHOME][PLACEMENT] -->
