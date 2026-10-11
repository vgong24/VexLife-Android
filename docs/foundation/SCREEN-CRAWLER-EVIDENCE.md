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

## Typed source API

### Full app in Simplified Chinese, large text

~~~kotlin
val config = vexScreenCrawlerConfig {
    allShards()
    languages(VexCrawlerLocales.ChineseSimplified)
    fontProfiles(VexCrawlerFonts.Large)
    capture(VexCrawlerCapturePolicy.SCREEN_ONCE)
}
~~~

### One Screen across English + Chinese and normal + large fonts

~~~kotlin
val config = vexScreenCrawlerConfig {
    screens(UserProfileScreen)
    languages(
        VexCrawlerLocales.EnglishUs,
        VexCrawlerLocales.ChineseSimplified,
    )
    fontProfiles(
        VexCrawlerFonts.Normal,
        VexCrawlerFonts.Large,
    )
    capture(VexCrawlerCapturePolicy.EVERY_STEP)
}
~~~

This becomes the Cartesian product of the selected journeys with the requested locale/font profiles.
The semantic walk remains single-source.

### Selected shards

~~~kotlin
val config = vexScreenCrawlerConfig {
    shards(
        DebugJourneys,
        GalleryJourneys,
    )
    languages(VexCrawlerLocales.ChineseSimplified)
    fontProfiles(VexCrawlerFonts.Large)
}
~~~

A Screen selection resolves every existing journey whose declared semantic map touches that Screen.
In Screen-selection mode the evidence observer captures only the selected Screen IDs, so route setup
Screens do not automatically become translation screenshots for the requested target.

## Opt-in instrumentation

Ordinary `:app:connectedDebugAndroidTest` does not expand the crawler matrix.

The dedicated `VexScreenCrawlerInstrumentedTest` produces a skipped placeholder unless:

~~~text
vexCrawlerEnabled=true
~~~

Crawler arguments:

~~~text
vexCrawlerShards
  comma-separated shard IDs

vexCrawlerScreens
  comma-separated stable screen IDs

vexCrawlerJourneys
  comma-separated <shardId>/<journeyId> refs

choose at most ONE selector above
omit all three -> all shards

vexCrawlerLocales
  comma-separated locale tags/aliases
  examples: en-US, zh-CN

vexCrawlerFonts
  comma-separated named profiles/scales
  examples: normal, large, 2.0

vexCrawlerCapture
  screen-once | every-step | final-only

vexCrawlerSourceRef
  source/build/commit evidence coordinate supplied by the caller
~~~

Example:

~~~bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.vexCrawlerEnabled=true \
  -Pandroid.testInstrumentationRunnerArguments.vexCrawlerScreens=screen.user-profile \
  -Pandroid.testInstrumentationRunnerArguments.vexCrawlerLocales=en-US,zh-CN \
  -Pandroid.testInstrumentationRunnerArguments.vexCrawlerFonts=normal,large \
  -Pandroid.testInstrumentationRunnerArguments.vexCrawlerCapture=every-step \
  -Pandroid.testInstrumentationRunnerArguments.vexCrawlerSourceRef=<source-ref>
~~~

## Locale authority

`VexCrawlerLocale` is only a test/evidence locale profile.

~~~text
CRAWLER_LOCALE_PROFILE
!=
PRODUCT_SUPPORTED_LANGUAGE
~~~

Chinese is present now only as a preparation/evidence profile. Until translated `zh` resources are
admitted, Android resource fallback is expected and the crawler must not claim Chinese localization
is complete.

When the localization foundation establishes a real SupportedLanguages owner, ScreenCrawler should
consume that owner rather than become a second product-language registry.

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
