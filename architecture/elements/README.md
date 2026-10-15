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
~~~

Existing registered cells such as `VexButton`, `VexIconButton`, `VexTextField`,
`VexScrollableColumn` and the generic `VexElementTree` remain part of the same Elements
foundation. Static container cells now also provide their registered element as the inherited parent
for nested Vex Compose content.

Do not create one-for-one wrappers for every Compose API merely for naming symmetry. Add a Vex
primitive when it carries recurring architectural meaning: inherited identity, semantic styling,
interaction ownership, QA/evidence hooks, or another repeated cross-feature contract.

### Gallery is the live Vex Compose catalog

Gallery is the first visual practicum for this foundation. Its custom modal realizations now enter
through their registered modal element and use nested Vex primitives whose IDs match the same nested
paths declared in the Gallery Blueprint.

This makes Gallery the place to inspect and pressure new sanctioned Vex Compose habits before they
spread through product Screens.

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
to make it testable.

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
