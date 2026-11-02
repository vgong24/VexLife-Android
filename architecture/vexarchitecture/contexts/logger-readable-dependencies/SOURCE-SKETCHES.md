# Source sketches — durable relational transcription

[VXG RealForever]

These are textual transcriptions of the three architecture sketches supplied in the conversation.
They preserve relational meaning independently of chat-image availability.

They are **SKETCH source**, not accepted Kotlin API.

Where the sketch itself contains inconsistent spelling or a "To Discuss" marker, that inconsistency is
preserved instead of silently corrected.

---

## Sketch A — VexDependencyFactory

Visual root:

~~~text
VexDependencyFactory
~~~

### RelationshipGraphs

~~~text
RelationshipGraphs

instanceMaps:
    Map<InstanceId, Instance>
~~~

### VexInjectible

The sketch says:

~~~text
interface VexInjectible

getDependencyId()
    = this::className
    // since its Family derived

build()
    = compiles dependencies
    + iterates through the graph of what has been created vs not yet
    + can instantiate them recursively
    + utilizes memoization
~~~

### Modules

~~~text
Modules

FamilyModule:
    Family Sets

AppModule:
    "Dependencies like shared repository store accesses"
~~~

### Classes

The sketch contains both spellings `VexInjectible` and `Injectable`:

~~~text
FamilyA implements "Injectable"

So that means <Family>
is injectable relationally by default
~~~

Do not normalize that spelling into API authority yet.

### Relationships

~~~text
Given the Module items:

derive dependencies from dependency modules first

then build out the Families in that order

so that Families can fill in what is already ready
~~~

### Explicit caution

The sketch term:

~~~text
Map<InstanceId, Instance>
~~~

is **not** automatically the rejected VexExecution process-global `InstanceId` from PR #206.

This sketch is talking about dependency-instance memoization/custody. The exact dependency-instance
key/name is TO DISCUSS and should not accidentally reopen execution occurrence identity.

Likewise:

~~~text
getDependencyId() = this::className
~~~

is a SKETCH, not a settled rule. Class name may be a readable coordinate, but this context does not
claim class-name identity is sufficient for every dependency relationship.

---

## Sketch B — UI / lifetime consumption

Visual root:

~~~text
UI Sample: VexApplication
~~~

### Application initialization

~~~text
InitializeDependencies()

using Singleton building approach
via VexDependencyFactory
~~~

### Lifetime layers

~~~text
VexAppScope
    Long running operations
    services
    broadcast receivers

VexActivity
    houses UI and lifecycles around the UI

VexScreen(id = "screenA")
    UI Composable Example
~~~

### Explicit Family dependency consumption

~~~text
VexRow(
    dependencyFamilyA: FamilyA
)
~~~

then:

~~~text
familyStateA =
    remember dependencyFamilyA.collectAsState()
~~~

then:

~~~text
VexUIComponentA(
    dependencyFamilyA,
    familyStateA
)
~~~

then:

~~~text
VexToggle(
    id = "buttonA"
    isActive = familyStateA.isActive
    onClick = dependencyFamilyA.activateFunction()
)
~~~

### Relational meaning preserved by the sketch

The visible direction is intentionally readable:

~~~text
application dependency construction
-> lifetime owner
-> Screen
-> row/component
-> Family dependency
-> observed Family State
-> UI action
-> Family activation
~~~

The sketch does not show a hidden service-locator lookup inside the component.

---

## Sketch C — VexLogger relationship sequence

Visual root:

~~~text
VexLogger(
    serverSideDeliveryOption: IDeliveryService? = null
)
~~~

This is a SKETCH. The optional delivery parameter is not yet accepted architecture.

### Logging Sequence

The sketch expresses:

~~~text
UI ButtonClicked
-> Family.Control(getIdFromComposable)
-> activate / log
~~~

then:

~~~text
fun activate(composableId, parameters) {
    log(composableId, Control)
}
~~~

then:

~~~text
admit
-> state phase
-> log(relationshipId, completionState)
~~~

The sketch explicitly marks that last part:

~~~text
To Discuss
~~~

Do not silently solidify `completionState`, its owner, or its relationship to `RelationshipId`.

The sketch also contains:

~~~text
fun log(elementId: String)
~~~

and:

~~~text
VexMetaData
    Common Placements
~~~

The exact role of `VexMetaData` is not yet defined by the sketch.

---

## Sketches as one relational sentence

Read together, the three sketches currently say:

~~~text
Readable dependency declarations
-> deterministic dependency graph
-> memoized live dependencies at explicit lifetimes
-> UI receives Family explicitly
-> UI observes Family State
-> UI action creates a semantic Control
-> Family/runtime accepts or observes facts
-> VexLogger records the causal chronology
-> optional downstream delivery/projection may follow
~~~

The precise interfaces are intentionally not frozen here.

<!-- [VXG RealForever][SOURCE-SKETCHES][LOGGER][DEPENDENCY-RELATIONSHIPS] -->
