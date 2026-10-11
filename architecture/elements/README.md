# Elements

[VXG RealForever]

This module exists so a page can be understood from one source file.

## Ordinary page

A normal page owns its identity and layout together:

~~~kotlin
object HomePage : PageBlueprint(
    pageId = "home",
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
~~~

The nested declaration generates nested element identities:

~~~text
screen.home
element.home.body
element.home.body.preview
element.home.body.preview.title
element.home.body.preview.message
~~~

Ordinary page rendering consumes this tree directly. Do not rebuild the same layout in a second Compose file.

## Named elements

Name an element only when another relationship needs to address it. Place that exact object in the layout.

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

Reusable identity and placed identity are therefore the same object, not two objects reconciled by a matching string.

## Repository shape

~~~text
pages/
  primary/
    home/
    events/
    settings/
  shared/
    debug/
    gallery/
~~~

A primary page is a peer destination. A shared page may be reached from more than one primary page and therefore does not belong under whichever caller happened to use it first.

## Boundaries

~~~text
PAGE BLUEPRINT = declared identity + structural placement
APP FRAME      = usable phone-safe coordinate space
RUNTIME        = current navigation + transient presentation
RENDERER       = Android realization of the blueprint

DECLARED PAGE != RUNTIME CURRENT PAGE
SYSTEM WINDOW != APP FRAME
RELATIONSHIP EXISTS != VALID NEXT GRAPH TRAVERSAL
~~~

<!-- [VXG RealForever][VEXLIFE-ANDROID][ELEMENTS] -->