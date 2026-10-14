# Vex Feature Template

[VXG RealForever]

> Copy the sections that are useful into the new feature's durable home. Delete sections that have
> no truthful distinct answer. This template is a thinking/continuity aid, not a documentation quota.

## Feature identity

~~~text
FEATURE / HOME:
CURRENT STATUS:
OWNER OF SEMANTIC MEANING:
CURRENT NEXT EDGE:
~~~

## Experience sentence

> A **[WHO]** comes to **[HOME]** because **[WHY]**.  
> **[WHAT]** tells them what domain things are known.  
> **[WHERE]** tells the experience where those things can come from, go to, or be realized.  
> **[HOW]** defines how the important behavior is structured.  
> **[NOW / WHEN]** explains what is true and possible now.  
> **[WHAT CAN I DO?]** names what may be requested.  
> **[WHO / WHAT REALIZES IT?]** names the rightful runtime owner.  
> **[WHAT DO THEY SEE?]** names the result/projection returned to the experience.

## Human question map

~~~text
HOME:
WHO:
WHY:
WHAT:
WHERE:
HOW:
NOW / WHEN:
WHAT CAN I DO?:
WHO / WHAT REALIZES IT?:
WHAT DO THEY SEE?:
~~~

## Category check

For every proposed top-level noun:

~~~text
NAME:
HUMAN QUESTION IT ANSWERS:
WHY IT IS DURABLE:
WHY AN EXISTING OWNER/NOUN CANNOT ANSWER IT:
PRODUCT MEANING OR TECHNICAL MACHINERY:
~~~

Delete/merge the noun if it does not earn a distinct answer.

## Current source map

~~~text
<draw the smallest useful source/ownership map here>
~~~

Start from where a fresh developer should enter, not from every file that happens to exist.

## Ownership map

~~~text
SEMANTIC MEANING:
CURRENT STATE:
DATA / SOURCE:
RUNTIME EFFECT:
ANDROID / PLATFORM:
AUTHORIZATION / PERMISSION:
PROJECTION / UI:
PROOF:
~~~

## Reused foundations / contracts

~~~text
FOUNDATION / CONTRACT:
WHY IT FITS:
WHAT THIS FEATURE DOES NOT OWN:
~~~

Do not duplicate a reusable contract locally merely to make the feature self-contained.

## Provider / integration map

~~~text
VEX-FACING CONTRACT:
PROVIDER:
ADAPTER:
VENDOR / NATIVE SDK:
LIFETIME OWNER:
FAILURE / UNAVAILABLE OWNER:
~~~

Keep vendor/runtime vocabulary below the semantic boundary unless vendor identity is product meaning.

## Human surfaces

~~~text
SCREEN / SURFACE:
WHAT IT PROJECTS:
WHICH CONTROLS IT EXPOSES:
WHAT IT DOES NOT OWN:
~~~

## Acceptance walks

### <human request>

~~~text
GIVEN
    <current Case / source / authorization facts>

WHEN
    <Control or human request>

THEN
    <selected semantic relationship>

REALIZED BY
    <Provider / Relay / repository / service / SDK owner>

OBSERVED AS
    <new truth / result>

EXPERIENCED AS
    <projection / visible outcome>

PROVED BY
    <focused + aggregate proof appropriate to the claim>
~~~

Repeat only for materially distinct walks.

## Test / proof map

| Claim | Owner | Proof |
|---|---|---|
| <claim> | <semantic/platform owner> | <JVM/Journey/render/device/lived/etc.> |

## Current holds / deliberate non-goals

~~~text
HELD:
WHY:
WHAT WOULD EARN IT:
~~~

Use this to prevent an explicitly deferred idea from silently becoming assumed architecture.

## Completion checklist

~~~text
[ ] Feature home matches current source
[ ] Non-obvious naming/boundary reasons preserved
[ ] Focused semantic tests pass
[ ] New/changed Journey added when warranted
[ ] Visual proof added only for visual claims
[ ] Native/provider proof used for native/provider claims
[ ] Appropriate vexVerifyJvm / vexVerifyLocal / vexVerifyDevice run
[ ] Generated/unexpected source effects reconciled
[ ] Foundation index changed only if a foundation really changed
[ ] PR/handoff states passed proof and owed proof separately
[ ] Stale experimental/duplicate routes removed or deliberately preserved
[ ] Next unfinished edge is explicit, or the admitted outcome is terminal
~~~

## Final handoff sentence

> **[Feature]** now lets **[who]** achieve **[outcome]** through **[semantic shape]**.  
> **[owners/providers]** realize the effect.  
> **[proof]** establishes **[claims]**.  
> **[held/next]** remains intentionally unresolved.

<!-- [VXG RealForever][VEX-FEATURE-TEMPLATE] -->
