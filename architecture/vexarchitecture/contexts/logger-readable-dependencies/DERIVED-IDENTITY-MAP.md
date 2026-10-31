# Derived identity map — host, meaning, content source, realization

[VXG RealForever]

> **BRIDGE DESIGN LAW**
>
> Do not require a human-authored ID when the architecture already owns enough relationship
> coordinates to derive the identity truthfully.
>
> The goal is one coherent identity map whose consequences can be filtered/projected for Logger,
> crawler, VexSync, VexBoard, localization verification, test tooling, and architectural conformance.

## 1. Identity follows meaning

Different UI things derive identity from different rightful relationships.

~~~text
ACTION ELEMENT

UI placement / host
+ semantic Control
+ typed ResourceId when the Control relationship requires it
-> derived action-element coordinate


CONTENT ELEMENT

UI placement / host
+ canonical content-source coordinate
-> derived content-element relationship


DYNAMIC PRODUCT ELEMENT

UI placement / host
+ feature-owned ResourceId
-> derived runtime presentation coordinate
~~~

Manual IDs remain available only when the existing meaningful coordinates are genuinely insufficient
to distinguish two lawful things.

Do not impose an arbitrary ID tax merely because some hypothetical future ambiguity could exist.

## 2. Host identity and content-source identity are separate

For text/content, keep:

~~~text
HOST
    where the content is shown

CONTENT SOURCE
    where the content meaning comes from

REALIZATION
    what the user actually sees for this locale/state/parameters
~~~

Example:

~~~text
host
    element.settings.body.connection-status

content source
    android:string/connection_status

realization
    "Connected"
~~~

The same content source may be reused lawfully:

~~~text
element.home.connection.banner ---------+
element.settings.connection.status -----+--> android:string/connection_status
element.debug.network.summary ----------+
~~~

No duplicated content ID is required merely because placement differs.

## 3. Native resource identity can become the content-source coordinate

On Android, app code may continue using the native resource reference:

~~~kotlin
R.string.connection_status
~~~

The architecture may derive a readable canonical resource coordinate from that native source rather
than asking the author to maintain another string ID.

Conceptually:

~~~text
@StringRes Int
-> native resource resolver
-> package/type/entry or stable entry form
-> Vex content-source coordinate
~~~

Example forms:

~~~text
com.vextreme.vexlife:string/connection_status
android:string/connection_status
string/connection_status
~~~

The exact transported form is a platform/protocol choice.

Do not make the generated Android integer resource value itself the durable cross-process/cross-build
protocol identity.

## 4. Styled text, plurals and derivatives do not require duplicate identity

The source identity remains the source identity while realization changes.

Styled example:

~~~text
content source
    string/download_complete

realization
    "Download complete"
    with "complete" bold
~~~

Plural example:

~~~text
content source
    plurals/download_count

realization inputs
    quantity = 1

realization
    "1 download"
~~~

or:

~~~text
same content source
    plurals/download_count

realization inputs
    quantity = 12

realization
    "12 downloads"
~~~

Formatting arguments, plural quantity, styling spans, emphasis, and other derivatives belong to
realization. They do not automatically create another content-source identity.

## 5. Action components may derive identity from Control

A Vex action component should not necessarily require:

~~~kotlin
VexButton(
    id = "close-dialog",
    control = Control.CloseDialog,
)
~~~

when the architecture already knows:

~~~text
current Screen
+ Blueprint parent placement
+ Control.CloseDialog
-> unique lawful host/action coordinate
~~~

Conceptually the API may become closer to:

~~~kotlin
VexButton(
    control = Control.CloseDialog,
)
~~~

with the host coordinate derived from placement and semantic action.

The exact Kotlin API remains implementation work; the law is the important part.

### Do not corrupt Control merely to make UI placement unique

If one dialog has:

~~~text
Header -> X button -> Control.CloseDialog
Footer -> Close button -> Control.CloseDialog
~~~

both buttons may lawfully mean the same Control.

Their UI coordinates differ because their host placement differs:

~~~text
dialog.header + CloseDialog
dialog.footer + CloseDialog
~~~

Do not invent:

~~~text
Control.CloseDialogHeader
Control.CloseDialogFooter
~~~

unless the product semantics actually differ.

Presentation topology answers **where**.
Control answers **what it means**.

## 6. Meaningful semantic distinctions should remain semantic

If two visually similar interactions mean different things, their Controls should differ because the
meaning differs, not because Logger needs unique IDs.

Examples:

~~~text
X on dialog
    Control.CloseDialog

toolbar back
    Control.GoBack

system Back gesture
    Control.GoBack

X on cancellable operation
    Control.Cancel
~~~

The visual glyph does not own the semantic identity.

## 7. One map, many filters

Do not create a new architecture for every conformance question.

Build the relationship map once, then derive/filter views.

Conceptually the map may contain relationships such as:

~~~text
host element
-> owning Screen
-> parent placement
-> element kind
-> semantic Control when actionable
-> content source when content-bearing
-> feature ResourceId when dynamic
-> observed Logger facts
~~~

Then ordinary functions/filters can answer:

~~~text
Which Vex elements have no resolvable content source?

Which observed elements could not be resolved to Blueprint structure?

Which product Screens still use a raw native component where a sanctioned Vex component exists?

Which declared actionable elements were never observed by the crawler?

Which observed IDs have no server/Blueprint interpretation?

Which text hosts rendered a value inconsistent with their declared source relationship?
~~~

The filter/report function is not another truth owner.

## 8. Crawler + Logger turns the map into conformance coverage

The earlier ScreenCrawler pattern remains valuable:

~~~text
Blueprint / traversal source
-> crawler explores lawful Screens/actions
-> normal runtime interaction executes
-> Logger records source + accepted/observed facts
-> mapping/resolution failures become Logger diagnostic facts
-> filter / grep / report returns all missing relationships from one run
~~~

This avoids repeated manual walks and avoids maintaining a second QA mapping system.

Example diagnostic fact:

~~~text
MappingMissing
    host = element.settings.body.connection-status
    expectedRelationship = ContentSource
~~~

or:

~~~text
BlueprintResolutionMissing
    host = element.settings.body.theme.dark
~~~

Logger exposes the inability to explain the relationship.
The rightful mapper/Blueprint/resource system remains responsible for the mapping itself.

## 9. Missing interpretation is itself an observable fact

Stable Logger law:

> If the architecture expects a relationship to be interpretable and the relationship cannot be
> resolved, that missing interpretation must be observable rather than silently degraded.

Examples:

~~~text
UI host exists but content source cannot resolve
-> Logger diagnostic fact

crawler observes actionable host not present in expected Blueprint map
-> Logger diagnostic fact

server/VexSync receives stable host coordinate but matching identity map is unavailable
-> Logger/server diagnostic fact
~~~

Do not silently substitute "unknown" and erase the gap.

## 10. Cross-platform relationship

The architectural concept is not Android `R.string`.

The portable relation is:

~~~text
host
-> canonical content source
-> realization
~~~

A platform adapter may expose its native source coordinate:

~~~text
Android
    package:string/connection_status
    package:plurals/download_count

another platform
    its own canonical localization/content-source coordinate
~~~

Vex keeps the relationship shape while allowing the platform to retain its rightful native source
system.

## 11. VexUI conformance law

The earlier idea:

~~~text
sanctioned Vex component
-> mandatory manually-authored id
~~~

can be reduced.

Preferred law:

~~~text
sanctioned Vex component
-> requires enough meaningful relationship inputs
-> identity derived from those inputs + placement
~~~

Examples:

~~~text
Vex action
    placement + Control (+ ResourceId when needed)

Vex text/content
    placement + canonical content source

Vex dynamic row
    placement + feature-owned ResourceId
~~~

If a sanctioned Vex equivalent exists and product code uses the raw native equivalent, lint/migration
tooling may flag that architectural escape.

If no sanctioned Vex primitive exists yet, do not force a meaningless wrapper merely to satisfy a
rule. The real repeated architectural need must first earn the Vex primitive.

## 12. Stable summary

~~~text
DO NOT:
    invent IDs because tooling wants IDs

DO:
    derive identity from relationships the architecture already owns


UI HOST
    placement topology

ACTION MEANING
    Control (+ typed ResourceId where required)

CONTENT SOURCE
    native canonical resource/source coordinate

DYNAMIC INSTANCE
    feature-owned ResourceId

REALIZATION
    localized/formatted/styled value

CHRONOLOGY
    VexLogger

HUMAN INTERPRETATION
    Blueprint/resource/VexSync/VexBoard projection
~~~

The strength of the map is that each coordinate remains owned by its rightful layer while the
relationships between them are inspectable.

<!-- [VXG RealForever][DERIVED-IDENTITY-MAP][VEXUI][VEXLOGGER][CRAWLER] -->
