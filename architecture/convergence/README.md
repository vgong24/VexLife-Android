# VexConvergence — retained CURRENT mechanics

[VXG RealForever]

## Human question

> When one rightful owner receives partial updates over time, how does it serialize those updates
> against one exact retained CURRENT and avoid publishing semantic no-ops without forcing that owner
> to pretend it is a Relay, Partner, Family, Store, provider, or execution runtime?

The reusable answer is deliberately small:

~~~text
one admitted Update
+ one exact retained CURRENT
-> one serialized non-suspending converge(Update, Current)
-> publish only when CURRENT meaning changes
~~~

VexConvergence owns only that mechanism.

It does not own dependency observation, VexExecution lifetime, Partner identity / Conversion /
Observation / Receiver, Family semantics, Relay InputState / OutputState, persistence, provider
freshness, defensive-copy policy, or partial invalidation policy.

Current concrete pressures:

~~~text
VexCompoundRelay
    independently-owned CURRENT inputs
    -> Relay-specific complete relationship reevaluation

VexStreamPlayerStore
    one Player Partner's admitted facet observations
    -> one coherent retained outside CURRENT
~~~

Those consumers share convergence mechanics, not semantic ownership.

<!-- [VXG RealForever][VEX-CONVERGENCE][FOUNDATION] -->
