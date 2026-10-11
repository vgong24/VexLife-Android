# Elements

[VXG RealForever]

This module answers one developer question:

> I need to create a UI thing. What do I define, and how does the rest of the app keep track of it?

## Ordinary use

Define the element once. Its declaration carries its stable identity, default style, visible-string
resource, icon when relevant, and machine-readable intention.

~~~kotlin
object Help : ButtonElement(
    elementId = "element.help",
    labelRes = R.string.help,
    style = VexElementStyles.SecondaryAction,
    intent = navigateTo(Screens.Help),
)
~~~

Place it in a blueprint:

~~~kotlin
row(ScreenElements.Actions) {
    button(ScreenElements.Help)
    button(ScreenElements.Save)
}
~~~

Render it without repeating its behavior:

~~~kotlin
VexButton(ScreenElements.Help)
~~~

The runtime action handler turns the declared intention into navigation, presentation, application
behavior, and logging. The call site does not manually repeat logger or navigation calls.

## The important separations

~~~text
ELEMENT DECLARATION = what this thing is
PLACEMENT           = where this blueprint places it
RUNTIME CONTEXT     = where the user actually is now
STYLE OVERRIDE      = how this placement may present the same declaration

ELEMENT_ID != SCREEN_CONTEXT
ELEMENT_ID != DISPLAY_LABEL
ELEMENT_ID != TEST_ONLY_TAG
BLUEPRINT_PARENT != RUNTIME_SCREEN
~~~

Shared-shell elements therefore stay shared. A Debug FAB can be declared once while runtime screen
context records whether it was pressed from Home, Events, or Settings.

## Small grammar

The first intention vocabulary is intentionally small:

~~~text
navigateTo(screen)
navigateBack()
open(element)
close(element)
toggle(element)
swapTo(element)
invoke(action)
intentA then intentB
~~~

Add another verb only when a real element cannot express its intention with the existing grammar.

## Placement

Blueprint layout is relational rather than coordinate-based:

~~~text
column
row
stack
scrollableColumn
dialog
swappable
~~~

Android Compose owns pixels and native rendering. The blueprint owns readable structural
relationships.

<!-- [VXG RealForever][VEXLIFE-ANDROID][ELEMENTS] -->
