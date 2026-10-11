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

Local/manual ScreenCrawler work is configured in one file:

~~~text
app/src/journeyTest/kotlin/.../testing/crawler/VexScreenCrawlerRunConfig.kt
~~~

A new developer should not need to learn Gradle/instrumentation argument names to run a crawler.

The preset is intentionally a comment/uncomment surface:

~~~kotlin
object VexScreenCrawlerRunConfig {
    const val enabled = false

    val config = vexScreenCrawlerConfig {
        // No selector means ALL current/future canonical shards.

        // screens(UserProfileScreen)
        // shards(DebugJourneys)
        // shards(DebugJourneys, GalleryJourneys)
        // allShardsExcept(DebugJourneys)

        languages(
            language("en"),
            // language("zh"),
            // language("bg"),
            // language("lt"),
            // language("zh-Hans"),
        )

        fontProfiles(
            VexCrawlerFonts.Normal,
            // VexCrawlerFonts.Large,
            // VexCrawlerFonts.ExtraLarge,
        )

        capture(VexCrawlerCapturePolicy.SCREEN_ONCE)
    }
}
~~~

To run:
1. set `enabled = true`;
2. optionally uncomment one typed selection line;
3. enable/disable language/font rows;
4. run `VexScreenCrawlerInstrumentedTest`.

The dedicated crawler test remains skipped while `enabled = false`, so ordinary Android test runs do
not accidentally expand screenshot batches.

## Kotlin identity / source-of-truth rules

### Shards

`VexJourneyCatalog.shards` is the one canonical shard list.

`VexJourneyCatalog.shardsById` is derived from that list for orientation/reporting only.

ScreenCrawler selection stores actual `VexJourneyShard` objects:

~~~kotlin
shards(DebugJourneys, GalleryJourneys)
~~~

not copied IDs:

~~~text
"shared.debug", "shared.gallery"
~~~

Default selection includes all canonical shards automatically. A new shard therefore becomes
crawlable as soon as it is added to `VexJourneyCatalog`; no crawler registry edit is required.

`allShardsExcept(DebugJourneys)` is the convenient form when the desired policy is "everything,
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

Artifacts are written under the target app's external-files area in a `vex-screen-crawler` tree.
A later host/export workflow may collect/package those artifacts without changing crawler semantics.

## Lifecycle / leak boundary

Crawler configuration and the shared journey catalog contain IDs, immutable values and test lambdas.
They must never retain:
- Activity;
- View;
- Compose rule/tree;
- Context as durable config state;
- screenshot Bitmap after the write completes.

The evidence observer:
- acquires target Context only while resolving app-scoped storage;
- obtains a screenshot Bitmap;
- writes PNG;
- calculates SHA-256;
- recycles the Bitmap;
- retains only strings/files/IDs afterward.

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
