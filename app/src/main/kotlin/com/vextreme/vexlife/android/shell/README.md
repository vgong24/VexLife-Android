# Shell — Stage + Deck

**Human situation:** a person using VexLife is usually doing one main thing (driving somewhere,
listening, talking with family) while wanting the others within reach. The app should put the
main thing in front and keep the rest one swipe away, without separate apps that cannot see each
other.

**Current truth:** the app is arranged in layers, back to front:

~~~text
Stage   the current Screen. Home is the stage Screen: the road while navigating, Vex at rest
Deck    one convergence sheet: Music · Journey · Family, with a handle (Tucked/Peek/Open/Full)
Nav     primary navigation; the FAB rides above the resting deck and steps aside when raised
~~~

Nothing in the shell owns feature truth. Every page and stage is a projection of an owner's glance:

| Glance | Owner | Used by |
|---|---|---|
| `VexStreamNowPlaying` (+ up next, skip availability) | VexStream Player Store + Playback Family | Music page |
| `VexNavigationGlance` (+ upcoming turns) | VexMaps Navigation session | Journey page, Map stage banner |
| `VexJourneyStage` (route + route-matched distance) | VexMaps Navigation session | Map stage |
| `VexConversationGlance` (+ recent lines) | Family Conversation Hybrid relay | Family page |

## The stage law (`VexStage.kt`, pure)

~~~text
VexLiveSet(navigation, nowPlaying, family)
-> navigating (ACTIVE/OFF_ROUTE/LOCATION_HELD/ARRIVED) ? Map stage : Home stage
-> pages always Music · Journey · Family (constant order; minus the page the current Screen owns)
-> preferred page
     Map stage:  Music if something is current, else Journey
     Home stage: Music if current, else a prepared route, else the latest family message, else Music
-> reveal
     Map stage:            Peek (the road stays visible)
     Home stage:           Open while music plays, else Peek
     off the stage Screen: Peek if anything is live, else Tucked
-> human choices stick within a situation (stage, on/off stage Screen); a new situation
   re-arranges once (vexDeckRearrangement)
~~~

## The deck (`VexDeck.kt`, `VexDeckPages.kt`)

- Built on the shared `VexContextDock` element (architecture/elements): hoisted
  `VexContextDockState`, `VexContextDockLevels.Layered`, labelled tabs, live dots, accessibility
  actions for show more / show less / tuck away. The dock's semantics state the selected page and
  reveal.
- Every human page or reveal change is recorded as a runtime interaction
  (`presentation.deck.page.<page>`, `presentation.deck.reveal.<level>`).
- Page actions are registered shell Elements (`AppShell.Elements.Deck*`). Play/pause and skip are
  finite requests to the Playback / Queue Families, the same path as the home-screen widget.
- VexMaps hosts its own dock (the map is its stage) with the same Journey and Family pages, tuckable
  but never Full.

## Proof

- `VexStageTest`: stage, preferred page, reveal, constant order, owned-page exclusion, re-arrangement.
- `VexContextDockTest`: drag / handle-tap reveal laws, compact compatibility.
- Glance tests: up next, upcoming turns, recent lines.
- `VexStageDeckVisualGoldenTest`: Map stage at Peek / Journey open / tucked (light), Home stage with
  music open / full / family full / nothing live, deck on Events and Family Conversation, Talk to Vex.
