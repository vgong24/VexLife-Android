# [VexMaps Complete] record — Provider reliability is a separate proof surface

[VXG RealForever]

Feature ref: feature.vexmaps  
Recorded at: **2026-10-16**  
Record ref: design.feature.vexmaps.2026-10-16.004  
Tags: **[VexMaps Complete] [Provider Reliability] [Mock Location] [Hybrid] [Test Foundation]**

## Pressure

VMC-01 destination-search source reached deterministic acceptance:

~~~text
focused JVM = PASS
vexVerifyLocal = PASS
connected Android = PASS
Fresh Independent Assurance = CLEAR
exact-current merge = PASS
~~~

The first lived target-device query then exposed a different truth:

~~~text
query = "in n out"
Android system Geocoder
-> no matching places found
~~~

That result does not retroactively invalidate the accepted source/currentness architecture.

It proves a missing distinction:

~~~text
DETERMINISTIC_CORRECTNESS
!=
LIVE_PROVIDER_USEFULNESS
~~~

A provider may be correctly integrated and still be unavailable, weak for a product query class,
rate-limited, regionally incomplete, or temporarily degraded.

## Existing Test / Proof Foundation already reused

Do not create another test registry.

Current repository truth already provides:

~~~text
feature ownership
    source/test packages

human-path sharding
    VexJourneyShard

VexMaps Journey shard
    shared.vex-maps

selection
    exact case
    shard
    environment
    baseline vs all-declared matrix

whole-local proof
    vexVerifyJvm
    vexVerifyLocal

whole-connected proof
    vexVerifyDevice
~~~

The new need is not "how do we shard Journeys?"

The new need is:

> Which proof may contact external/live reality, how often, under what budget, and whether that
> result is a merge gate, scheduled health signal, or lived acceptance predicate?

## Proof classes

### 1. Owner-deterministic proof

Runs during ordinary source development and every relevant PR.

Examples:

~~~text
pure reducers
Session/currentness
provider translation with fixed input objects
Compose render contracts with fixed state
repository/storage semantics
route matching
~~~

Properties:

~~~text
no external API budget
repeatable
cheap
required on exact candidate
~~~

### 2. Connected-native proof

Runs when Android/provider substrate owns the claim.

Examples:

~~~text
permissions
AndroidView / MapLibre
Service / notification
MediaSession
Room/device integration
Android Address -> VexMaps result projection
OS-level location-provider integration
~~~

This may use a physical device or emulator but should remain deterministic where the claim permits.

### 3. Live-provider reliability probe

An explicitly opt-in test/probe that is allowed to contact live external/provider reality.

Examples:

~~~text
real place/business search
real route API
remote VexHome capability
remote companion handshake
~~~

Properties:

~~~text
external dependency
budgeted
timestamped
provider-specific
failure retained as provider evidence
not silently retried until green
not run on every ordinary PR by default
~~~

A live probe is not a replacement for owner tests.

### 4. Lived human acceptance

The final human question:

~~~text
does this feature actually work/usefully behave on the target device for the intended task?
~~~

Examples:

~~~text
"In-N-Out" can be discovered without exact address spelling
selecting a result feels understandable
navigation starts from the expected place
Distribution is discoverable without dominating ordinary Maps
~~~

This may reveal product failure even when all automated proof is green.

## Cadence

### During implementation

Run the cheapest owner proof first.

~~~text
focused test classes
-> feature render/device case when needed
~~~

### Feature checkpoint

Use the existing feature shard where Journey proof exists:

~~~text
shared.vex-maps
~~~

Do not add another feature registry merely for tests.

### Final exact-head step before READY / merge

The exact currentized head must run the repository-wide deterministic composition proofs, not only the
feature-focused shard:

~~~text
vexVerifyLocal
+
vexVerifyDevice
+
other currently-admitted deterministic proof surfaces when implicated
~~~

This is the "all tests before next merge step" boundary.

Focused/sharded tests accelerate development; they do not replace the final exact-head aggregate.

### Provider-changing PR

When a PR changes the provider contract or adopts a new provider, run the provider's live probe before
claiming provider acceptance.

If the provider environment is unavailable, hold only the provider-usefulness claim. Do not relabel
deterministic source proof as failed.

### Scheduled reliability

A qualified host/CI may call the repository-owned live-probe entry point on a bounded cadence such as:

~~~text
weekly
~~~

The scheduler owns time only.

The repository-owned test/probe source owns:

~~~text
which provider
which fixture set
request budget
acceptance semantics
evidence format
~~~

Hosted workflow configuration must not become a second test-definition authority.

### Pre-release

Run:

~~~text
all deterministic local proof
all deterministic connected proof
admitted visual/Journey proof
all qualified live-provider probes
required lived product walks
~~~

This is broader than an ordinary PR merge gate.

## API budget law

Live probes must be intentionally small.

A provider-specific probe declares:

~~~text
fixture set version
maximum requests per run
timeout
retry policy
expected capability class
~~~

Default direction:

~~~text
no hidden retry loops
no autocomplete flood
no random query generation
fixed bounded query fixtures
failure remains evidence
~~~

A retry may establish recovery; it does not erase the earlier failure.

The Test / Proof Foundation incident law continues to apply.

## Search reliability fixture classes

Destination-search providers should be tested against the promises VexMaps actually wants.

Candidate fixture classes:

~~~text
ADDRESS
    known full public address

PLACE / LANDMARK
    named city/airport/landmark

BUSINESS / POI
    ordinary human query such as a restaurant/store name

PARTIAL / SUGGESTION
    only for a provider whose accepted contract includes autocomplete/partial discovery

LOCATION-BIASED AMBIGUITY
    only after the provider contract accepts proximity/bias input
~~~

A provider that passes address geocoding but fails business/POI discovery may remain useful as a
fallback while being rejected as the ordinary VexMaps search provider.

The Android system Geocoder is therefore treated as a first/bootstrap provider, not permanent provider
lock-in.

## Location testing has three different strengths

Do not call all of these "mock location."

### A. Deterministic source fixture

~~~text
test-owned location source
-> known coordinate/sample
-> feature/domain proof
~~~

Use for cheap deterministic semantics.

No Hybrid relationship is needed merely because a fake value exists.

### B. Maps Lab location override

This is where Hybrid becomes earned.

~~~text
canonical observed device location
+
explicit noncanonical Maps Lab location override
        |
        v
VexMaps<LocationQuestion>HybridRelay
        |
        v
effective test/debug location + provenance
~~~

Required Hybrid laws:

~~~text
canonical location continues advancing underneath
override does not rewrite canonical provider truth
provenance remains inspectable
clearing override reveals latest canonical location
one relationship owner defines precedence
~~~

This would support interactive/debug pressure such as:

~~~text
pretend I am at test anchor X
while real GPS continues underneath
~~~

Hybrid is not shorthand for mock.

Do not introduce a generic VexHybridRelay locally: the accepted repository currently has the
Conversation Hybrid consumer but no accepted reusable Hybrid foundation type. A real Maps Hybrid
consumer would be second-consumer pressure for the Relay foundation.

### C. Android OS test provider

For stronger native proof, configure Android's test-location provider so the existing production
VexDeviceLocationSource consumes the OS location path.

Android exposes test-provider APIs / shell controls for this purpose, and framework Locations from a
test provider are identifiable as mock locations.

This proves more of the real substrate than an app-injected fake:

~~~text
Android test provider
-> LocationManager
-> VexDeviceLocationSource
-> Maps/navigation owner
-> visible behavior
~~~

Hybrid is not required for this route because the mock enters as an Android provider observation.

Cleanup is mandatory.

A failed/aborted test must not leave the physical device pinned to a test location.

## Private location fixtures

Do not commit Victor's home address or exact private coordinates into repository source, snapshots,
logs, or public/provider fixture names.

Use a private host/device profile:

~~~text
fixtureRef = maps.private.home-anchor
latitude/longitude = supplied locally at execution time
~~~

Repository evidence should prefer:

~~~text
fixtureRef
provider/probe version
distance/radius assertions
result count/latency
~~~

over raw private coordinates.

Public addresses/landmarks should be preferred for ordinary provider reliability fixtures.

If an exact private-address query is ever required, keep the query host-local and redact it from
durable evidence unless Victor explicitly chooses otherwise.

## Relationship to Journey

Journey may:

~~~text
change rightful source truth
exercise registered human actions
observe the product result
~~~

Journey must not author Hybrid/Convergence output directly.

A Maps Lab override, if admitted, is changed through its rightful Lab/source owner.

An Android OS mock-location probe is environment/provider setup outside the Journey's semantic action
stream; the Journey then walks the ordinary product UI.

## Relationship to current Browse/Search UX work

The live Geocoder result exposed two product requirements already under VexMaps Complete:

~~~text
forgiving partial/business discovery
up to ~20 scrollable suggestions/results within keyboard-safe height
~~~

The current Android Geocoder first provider is not assumed to satisfy those promises.

The next search-provider candidate should be evaluated with the live-provider fixture classes above
before it becomes the ordinary VexMaps search provider.

## Foundation promotion rule

Keep the first implementation feature-local.

Promote a reusable Test / Proof Foundation concept only after another real feature needs the same
meaning.

Likely future promotion candidates:

~~~text
LIVE_PROVIDER_PROBE cadence/metadata
bounded provider request budget
provider-probe evidence schema
private runtime fixture reference
~~~

Do not promote Maps query vocabulary, Maps location vocabulary, or one provider's contract.

## Current consequence

[VexMaps Complete] now carries two parallel completion questions:

~~~text
PRODUCT
    Browse/Search usability
    -> better discovery provider
    -> route preview
    -> navigation

RELIABILITY
    deterministic owner proof
    -> exact-head aggregate proof
    -> bounded live-provider probe
    -> lived human acceptance
~~~

Neither replaces the other.

<!-- [VXG RealForever][feature.vexmaps][VEXMAPS-COMPLETE][PROVIDER-RELIABILITY][DESIGN-RECORD-004] -->
