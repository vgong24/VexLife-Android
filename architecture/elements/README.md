# Elements

[VXG RealForever]

This module exists so a screen can be understood from one source file.

## Ordinary screen

A normal screen owns its identity and layout together:

~~~kotlin
object HomeScreen : ScreenBlueprint(
    screenKey = "home",
    titleRes = R.string.home_title,
) {
    override val content = layout {
        scrollableColumn("body") {
            card("preview") {
                text("title", R.string.home_preview_title, TextRole.Title)
                text("message", R.string.home_preview_body, TextRole.Supporting)
            }
        }
    }
}

@VexScreenPreviews
@Composable
private fun HomeScreenPreview() {
    VexBlueprintScreenPreview(HomeScreen)
}
~~~

The nested declaration generates nested element identities:

~~~text
screen.home
element.home.body
element.home.body.preview
element.home.body.preview.title
element.home.body.preview.message
~~~

Ordinary screen rendering consumes this tree directly. Do not rebuild the same layout in a second
Compose file.

The Preview entry stays in the SAME *Screen.kt file so a worker can edit structure and inspect the
visual result without hunting a parallel preview tree.

## Named elements

Name an element only when another relationship needs to address it. Place that exact object in the
layout.

~~~kotlin
val ShowLog = buttonRef(
    id = "body.log-history.actions.show",
    labelRes = R.string.log_history_show,
    intent = open(LogDialog),
)

row("actions") {
    button(ShowLog)
}
~~~

Reusable identity and placed identity are therefore the same object, not two objects reconciled by a
matching string.

Semantic journey coverage is now one legitimate addressing relationship. If a user-actionable
element must be exercised by the app journey map, give it a named ref rather than making the test
reconstruct its generated string ID. This is why Gallery's sample Button/Toggle/FAB are named even
though their product effects are intentionally small.

The rule remains need-based: static one-off text/layout nodes do not become named fields merely
because tests exist.

## Vex Compose — inherited presentation continuity

The Blueprint DSL already forms nested semantic identity from structure:

~~~text
scrollableColumn("body")
  -> card("preview")
      -> text("title")

element.<screen>.body.preview.title
~~~

Custom Compose must follow the same law. Product presentation should not fall back to isolated raw
Compose primitives whose identity, styling and QA hooks depend on each caller remembering them.

### VexModifier is the Compose root

`VexModifier` is a real Compose `Modifier`:

~~~kotlin
VexModifier
    .fillMaxWidth()
    .padding(8.dp)
    .clickable { ... }
~~~

It is not a parallel modifier value/configuration object. It is the Vex-owned architectural entry
point into the ordinary Compose modifier chain.

Registered/static identity may be attached with:

~~~kotlin
VexModifier.vex(SomeScreen.Save)
~~~

An already-formed runtime presentation ref may be attached with:

~~~kotlin
VexModifier.vex(runtimePresentationRef)
~~~

Raw Compose `testTag(...)` realization stays inside the Elements foundation.

### Nested Vex Compose scope

A custom renderer enters one semantic parent:

~~~kotlin
VexComposeScope(parent = SomeScreen.Panel) {
    VexColumn("search") {
        VexText(
            id = "title",
            text = title,
            role = TextRole.Title,
        )
    }
}
~~~

The child declarations are local. The full addresses are inherited:

~~~text
SomeScreen.Panel
  element.some-screen.body.panel

VexColumn("search")
  element.some-screen.body.panel.search

VexText("title")
  element.some-screen.body.panel.search.title
~~~

The Blueprint DSL and Vex Compose use the same path-normalization function. A slash/whitespace/path
rule therefore cannot silently mean one thing in registration and another in custom rendering.

A Vex primitive rendered without a Vex parent fails closed instead of inventing a detached ID.

### Current sanctioned core

The first earned custom primitives are deliberately small:

~~~text
VexModifier
VexComposeScope
VexText
VexColumn
VexRow
VexBox
VexCard
VexExplainableStatusIcon
~~~

Existing registered cells such as `VexButton`, `VexIconButton`, `VexTextField`,
`VexScrollableColumn` and the generic `VexElementTree` remain part of the same Elements
foundation. Static container cells now also provide their registered element as the inherited parent
for nested Vex Compose content.

A real consumer may already own a more specific Compose typography choice than a broad semantic
`TextRole`. `VexText` therefore keeps role-derived typography as the default while permitting an
explicit `TextStyle` override. That override preserves product presentation; it does not create a
second semantic identity or text-role owner.

Do not create one-for-one wrappers for every Compose API merely for naming symmetry. Add a Vex
primitive when it carries recurring architectural meaning: inherited identity, semantic styling,
interaction ownership, QA/evidence hooks, or another repeated cross-feature contract.


`VexExplainableStatusIcon` owns one such repeated interaction contract:

~~~text
decorative icon
    no independent interaction/meaning

action icon
    performs its named action

explainable status icon
    represents caller-owned state
    tap or long-press -> explain that state
    explanation does not mutate the represented state
~~~

The component deliberately does not own product wording or status truth. Features provide those.
It reuses `VexAnchoredOverlay` so explanation presentation remains attached to the inspected target.
The overlay resolver keeps the popup safe inside the window while shifting its caret toward the
anchor center after edge clamping. Do not recreate feature-local tooltip/status wrappers merely to
change text or icon choice.

### Gallery is the live Vex Compose catalog

Gallery is the first visual practicum for this foundation. Its custom modal realizations now enter
through their registered modal element and use nested Vex primitives whose IDs match the same nested
paths declared in the Gallery Blueprint.

This makes Gallery the place to inspect and pressure new sanctioned Vex Compose habits before they
spread through product Screens.

PR #88 graduated the first product consumer as well: VexStream Search enters through a registered
sheet, derives new nested structure from that parent, preserves pre-existing stable Search field/action
IDs through registry relationships, and addresses dynamic track rows through feature-owned runtime
presentation refs. A013 proved that exact foundation on repository-local verification and a physical
connected Android run.

### Migration visibility

During the POC/migration period, raw Compose use under product Screen source is visible through:

~~~bash
./gradlew :app:vexComposeMigrationReport
~~~

This is intentionally warning-only while Gallery and VexStream prove the sanctioned vocabulary.
Promote it to a hard source-alignment gate only after the rightful Vex replacement exists; the report
must not force authors to invent meaningless wrappers merely to satisfy a regex.

### Migration law

~~~text
raw product Compose
-> identify the semantic parent/question
-> enter VexModifier / VexComposeScope
-> use sanctioned Vex primitives
-> preserve provider/platform mechanics underneath
~~~

Vex does not replace Compose. Compose remains the Android rendering engine. Vex supplies the
architectural lattice that keeps product presentation discoverable, addressable and consistently
instrumentable.

Dynamic product data remains dynamic and must not be registered as fake static Screen elements merely
to make it testable. When a feature already owns a lawful full runtime presentation ref, that ref may
become the Vex Compose parent directly:

~~~text
feature-owned runtime presentation ref
-> VexComposeScope(parentRef = runtimeRef)
-> VexBox("artwork")
-> VexColumn("copy")
   -> VexText("title")
~~~

The feature remains the owner of runtime instance identity; Vex Compose owns only the nested
presentation-continuity law. Do not invent a generic dynamic-ID registry merely to make these two
systems look symmetrical.

## Deterministic semantic projection

`UIBlueprintRegistry.semanticProjection()` exposes the declared UI lattice as immutable data without
making the Elements module own a transport format.

Current projection includes:

~~~text
screens
  screenRef
  rootElementRef
  stable-ref-sorted owned element refs

elements
  semanticRef
  kind
  resolved semantic style
  owner Screen
  parent
  declaration-ordered children
  semantic icon
  text role
  optional Android text resource id
  text-field readOnly
  swappable default child
  typed intent projection

shell element refs
~~~

Top-level screen/element collections are stable-ref sorted for deterministic transport diffs.
Structural child order and Sequence intent order remain authored order because order is meaning.

The projection is deliberately serialization-neutral:

~~~text
UIBlueprintRegistry
-> UIBlueprintProjection
-> app/tooling encoder
-> JSON / VexBoard / diagnostics / another consumer
~~~

Do not add a JSON dependency to the portable Elements foundation merely to satisfy one consumer.
The app/tooling edge that knows source revision, localized human labels, current runtime state, and
target-specific adoption facts owns the transport envelope.

This projection is an Android realization projection, not a portable Origin registry. In particular:

~~~text
TextElement
  textResourceIdOrNull != null
    -> Android target-resource mapping exists

  textResourceIdOrNull == null
    -> current value must be supplied by the rightful Screen/runtime binding when rendered

both
  -> kind = Text
  -> same element identity model
~~~

An app/tooling exporter that has Android Resources access may turn the numeric resource id into an exact
symbolic target coordinate such as `R.string.home_vexstream_music`. That symbolic Android coordinate
can then be mapped to the corresponding content Origin during reverse adoption or forward construction.
The Elements module should not import VexBoard or a universal content registry merely to make that
mapping.

Permanent:

~~~text
DECLARED_SEMANTIC_PROJECTION
!=
RUNTIME_NAVIGATION_STATE
!=
RESOLVED_PIXEL_LAYOUT
!=
OBSERVED_SCREENSHOT_EVIDENCE
!=
WIRE_FORMAT
~~~

Dynamic domain occurrences remain feature-owned runtime presentation refs. The registry projection
does not invent them as fake static elements.

## Repository shape

~~~text
screens/
  primary/
    home/
    events/
    settings/
  shared/
    debug/
    gallery/
    profile/
~~~

A primary screen is a peer destination. A shared screen may be reached from more than one primary
screen and therefore does not belong under whichever caller happened to use it first.

## Boundaries

~~~text
SCREEN BLUEPRINT = declared identity + structural placement
SCREEN REGISTRATION = screen + root placement consumed by registry
APP FRAME        = usable phone-safe coordinate space
RUNTIME          = current navigation + transient presentation
RENDERER         = Android realization of the blueprint

DECLARED SCREEN != RUNTIME CURRENT SCREEN
SYSTEM WINDOW != APP FRAME
RELATIONSHIP EXISTS != VALID NEXT GRAPH TRAVERSAL
~~~

<!-- [VXG RealForever][VEXLIFE-ANDROID][ELEMENTS] -->
