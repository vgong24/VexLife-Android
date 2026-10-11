# ScreenCrawler Evidence Matrix

[VXG RealForever]

## Question

> How can we reuse the accepted semantic journey map to capture visual evidence under selected
> languages, font/accessibility conditions, shards, Screens or exact journeys without making the
> crawler or environment matrix part of every normal functional test run?

ScreenCrawler is an **opt-in evidence runner** layered over VexJourney.

It does not own:
- product navigation;
- product-supported languages;
- accessibility requirements;
- semantic expected-history meaning;
- regulatory acceptance.

## Normal journey runs stay functional-first

The ordinary Android journey suite now defaults to:

~~~text
one baseline environment
x
every semantic journey
~~~

That keeps the normal production feedback loop focused on functional behavior.

~~~text
./gradlew :app:connectedDebugAndroidTest
-> baseline only
~~~

Environment multiplication is explicit:

~~~text
vexJourneyMatrix=all
-> all environments declared by each journey

vexJourneyEnvironment=large-text-1.45x
-> only the declared large-text cases

vexJourneyCase=<caseRef>
-> one exact case
~~~

Example all-declared matrix:

~~~bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.vexJourneyMatrix=all
~~~

Localization/accessibility evidence is therefore a batch decision, not an automatic tax on every
functional run.

## ScreenCrawler relationship

~~~text
VexJourneyCatalog
  accepted semantic user walks
        |
        v
VexScreenCrawlerConfig
  selection:
    all shards
    selected shards
    selected Screens
    exact journey refs

  x locale profiles
  x font profiles
  x capture policy
        |
        v
VexScreenCrawlerPlanner
        |
        v
isolated VexScreenCrawlerCase values
        |
        v
VexTestEngine
  real Compose/runtime/logging verification
        +
VexScreenCrawlerEvidenceObserver
  screenshots + JSONL manifest + SHA-256
~~~

The crawler reuses the accepted interactions rather than constructing another app-walk language.

## One human control surface

For ordinary local ScreenCrawler work, a developer should know exactly one file:

~~~text
app/src/androidTest/kotlin/com/vextreme/vexlife/android/testing/crawler/
  VexScreenCrawlerInstrumentedTest.kt
~~~

That same runnable class contains:
1. where/how to right-click-run the crawler in Android Studio;
2. the `enabled` flag;
3. typed Screen/shard selection examples;
4. locale/font/capture configuration;
5. physical-device/emulator behavior;
6. macOS/Linux + Windows PowerShell `adb pull` commands;
7. evidence-path/manifest explanation.

There is no separate human run-config class.

The intended workflow is:

~~~text
open VexScreenCrawlerInstrumentedTest.kt
-> enabled = true
-> optionally uncomment ONE typed selection line
-> enable/disable locale/font rows
-> right-click Run
-> copy screenshots with the documented adb pull command
~~~

While `enabled = false`, the dedicated crawler test emits one explicit green no-op status and returns
without creating crawler cases or screenshot work. Disabled optional evidence therefore cannot make
ordinary Android test runs red or block development.

The lower-level planner/config/evidence classes are framework internals. A developer only needs them
when changing how ScreenCrawler itself works, not when choosing what to crawl.

Screenshot retrieval is also repo-owned:

~~~text
macOS/Linux:
  bash tools/pull-vex-screen-crawler.sh

Windows PowerShell:
  powershell -ExecutionPolicy Bypass -File .\tools\pull-vex-screen-crawler.ps1
~~~

The scripts derive the applicationId, use the sole connected adb target when unambiguous, verify the
artifact root, and pull evidence to `./vex-screen-crawler`. Multiple-device ambiguity is surfaced
instead of guessed.

## Kotlin identity / source-of-truth rules

### Shards

`VexJourneyCatalog.shards` is the one canonical shard list.

`VexJourneyCatalog.shardsById` is derived from that list for orientation/reporting only.

ScreenCrawler selection stores actual `VexJourneyShard` objects:

~~~kotlin
shards(Journeys.debug, Journeys.gallery)
~~~

not copied IDs:

~~~text
"shared.debug", "shared.gallery"
~~~

Default selection includes all canonical shards automatically. A new shard therefore becomes
crawlable as soon as it is added to `VexJourneyCatalog`; no crawler registry edit is required.

`allShardsExcept(Journeys.debug)` is the convenient form when the desired policy is "everything,
including future shards, except these temporary exclusions."

### Screens

Screen selection stores the actual registered `ScreenElement` / `AppScreenBlueprint` object:

~~~kotlin
screens(UserProfileScreen)
~~~

The planner validates it against `AppScreens`. There is no duplicated screen-ID configuration map.

### Languages / locales

Locale identity is different from Screen/shard identity: BCP-47 already defines the external code.

Use a validated typed value:

~~~kotlin
language("en")
language("zh")
language("bg")
language("lt")
language("zh-Hans")
language("pt-BR")
~~~

Do not create English-named Kotlin properties such as `ChineseSimplified` or `Bulgarian` merely
to represent standard language codes.

`VexCrawlerLocale` is still only a test/evidence condition:

~~~text
CRAWLER_LOCALE
!=
PRODUCT_SUPPORTED_LANGUAGE
~~~

When a localization foundation establishes the real supported-language authority, ScreenCrawler
should consume those canonical codes rather than become another language registry.

## Selection examples

### Full current/future app in Chinese, large text

~~~kotlin
val config = vexScreenCrawlerConfig {
    // no selector = all canonical shards
    languages(language("zh"))
    fontProfiles(VexCrawlerFonts.Large)
}
~~~

### User Profile only across English + Chinese and normal + large fonts

~~~kotlin
val config = vexScreenCrawlerConfig {
    screens(UserProfileScreen)
    languages(
        language("en"),
        language("zh"),
    )
    fontProfiles(
        VexCrawlerFonts.Normal,
        VexCrawlerFonts.Large,
    )
    capture(VexCrawlerCapturePolicy.EVERY_STEP)
}
~~~

A Screen selection resolves every existing journey whose declared semantic map touches that Screen.
In Screen-selection mode the evidence observer captures only selected Screen IDs, so route setup
Screens do not automatically become translation screenshots.

### Debug + Gallery only

~~~kotlin
val config = vexScreenCrawlerConfig {
    shards(
        DebugJourneys,
        GalleryJourneys,
    )
    languages(language("zh"))
    fontProfiles(VexCrawlerFonts.Large)
}
~~~

## Functional tests versus crawler evidence

The accepted ordinary journey suite remains baseline-only by default:

~~~text
connectedDebugAndroidTest
-> one baseline case per semantic journey
~~~

ScreenCrawler is a separately editable/opt-in evidence plan. It can multiply selected journeys across
language/font conditions without imposing that matrix on normal functional development.

## Font profiles

Current values are exploratory conditions:

~~~text
small       0.85x
normal      1.0x
large       1.45x
extra-large 2.0x
~~~

They are not medical/accessibility regulatory thresholds.

A future quality/regulatory requirement must name its authoritative standard, required configurations,
device/OS scope, evidence retention rules and acceptance criteria. ScreenCrawler can execute/capture
those conditions once admitted.

## Evidence artifacts

The Android evidence observer captures full-device PNGs with UiAutomation and appends one JSONL
manifest entry for each screenshot.

Current metadata includes:

~~~text
sourceRef
caseRef
shardId
journeyId
localeId / localeTag
fontId / fontScale
capturePolicy
screenId
stepIndex / stepKind / semanticRef
deviceModel / sdkInt
screenshot filename
screenshot SHA-256
~~~

This improves traceability; it is not by itself regulatory compliance.

Artifacts are written through AndroidX PlatformTestStorage under a `vex-screen-crawler/` relative
output tree. Modern instrumented tests run inside a storage sandbox, so an app-visible /storage path
must not be assumed to be adb-shell-visible after the test process exits.

The AndroidX test/Gradle infrastructure owns transfer back to the host. The repo helper then collects
returned crawler output from `app/build` into `./vex-screen-crawler`.

## Lifecycle / leak boundary

Crawler configuration and the shared journey catalog contain IDs, immutable values and test lambdas.
They must never retain:
- Activity;
- View;
- Compose rule/tree;
- Context as durable config state;
- screenshot Bitmap after the write completes.

The evidence observer:
- uses PlatformTestStorage as the instrumentation-output boundary;
- obtains a screenshot Bitmap;
- encodes PNG bytes in memory;
- calculates SHA-256;
- writes PNG/manifest/start/complete outputs through test storage;
- recycles the Bitmap;
- retains only semantic metadata afterward.

Production Room uses `applicationContext`; VexLifeSession is retained by AndroidViewModel /
`viewModelScope`. No current crawler/catalog path retains an Activity.

## Selection / planning proof

JVM planner tests cover:
- full baseline plan;
- one Screen across multiple locales/fonts;
- selected Debug + Gallery shards;
- unknown shard fails instead of silently producing an empty run.

The planner is pure JVM logic; Android is required only for actual journey execution and screenshot
evidence.

<!-- [VXG RealForever][VEXLIFE-ANDROID][SCREEN-CRAWLER][EVIDENCE-MATRIX] -->
