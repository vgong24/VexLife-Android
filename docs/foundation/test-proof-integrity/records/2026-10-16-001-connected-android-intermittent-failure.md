# Test / Proof Integrity record — Connected Android intermittent failure

[VXG RealForever]

Record ref: `integrity.test-proof.2026-10-16.001`  
Recorded at: **2026-10-16**  
Current status: **MONITORING_AFTER_HARDENING**  
Root cause: **UNKNOWN**

## Trigger

During exact-head proof for VexStream PR #98:

~~~text
focused VexStreamQueueBehaviorTest = PASS
vexVerifyLocal                    = PASS
vexVerifyDevice                   = FAIL
same exact candidate retry        = PASS
source changed between attempts   = NO
~~~

The first failing connected run reached:

~~~text
Samsung SM-S908U
Android 16
:app:connectedDebugAndroidTest
~~~

but the then-current proof gate retained only the generic Gradle statement that there were failing
tests. It did **not** retain the failing testcase identity.

## Latent risk

If the retry PASS had been treated as if the first failure never happened:

~~~text
intermittent foundation failure
    -> dismissed as noise
    -> no recurrence memory
    -> more features rely on same proof substrate
    -> future recurrence looks newly introduced
    -> current feature may be blamed
    -> larger downstream refactor becomes necessary
~~~

The debt vector was therefore not only "a flaky test."

It was:

> the proof foundation could exhibit intermittent failure while losing enough evidence that the
> repository could not recognize recurrence.

## Why it could have been missed

The ordinary lifecycle pressure strongly rewards:

~~~text
red
-> retry
-> green
-> continue
~~~

Without a durable integrity record, a later green result can retroactively erase the importance of the
red one.

This is especially dangerous in AI-heavy development because an AI can rationalize:

> "The exact source now passes, therefore the previous result was environmental noise."

That conclusion was not supported by the evidence.

## Observed evidence

### PR #98 A001

~~~text
head = f0e36a3d30e4351101217b482856e8c7316e4404
tree = 6bdc9d2be3770f96a1f80f8fd835dd82bbcb3f59

focused Queue JVM = PASS
vexVerifyLocal     = PASS
vexVerifyDevice    = FAIL

device             = Samsung SM-S908U / Android 16
source mutation    = NONE
failing testcase   = NOT RETAINED
~~~

### PR #98 A002

Same exact candidate:

~~~text
vexVerifyDevice    = PASS
instrumentation    = 10 tests
source mutation    = NONE
differential       = CANDIDATE_PASS
~~~

This proved only:

> PR #98 was not demonstrated to be the cause of the first failure.

It did **not** prove why A001 failed.

## Correlating signal

At the same time, the repository emitted Compose-test deprecation guidance for:

~~~text
VexJourneyInstrumentedTest
VexScreenCrawlerInstrumentedTest
~~~

Both still used v1 `createComposeRule`, whose test dispatcher behavior differed from the current v2
API.

That was a plausible reliability pressure, but it was not sufficient evidence to label the historical
root cause.

## Immediate disposition

~~~text
candidate regression = NOT ESTABLISHED
foundation intermittency = OBSERVED
historical root cause = UNKNOWN
retry history = PRESERVED
~~~

Source was not changed merely to make the original red result disappear.

## Systemic hardening

Current-main successor PR #100 formed three repairs:

1. **Failure observability**
   - connected instrumentation always finalizes into result inspection;
   - exact failing class/test/type/message is emitted when XML exists;
   - `device-result-summary.txt` is formed on PASS and FAIL.

2. **Evidence retention**
   - connected JUnit XML/report + summary are retained in the Test Foundation proof bundle.

3. **Compose test realization**
   - Journey and ScreenCrawler test rules migrate from deprecated v1 to v2 `createComposeRule`;
   - this is preventive hardening, not retrospective root-cause certification.

PR #100 controlled-red proof subsequently demonstrated that the new inspector can retain:

~~~text
VexJourneyInstrumentedTest#initializationError
java.lang.IllegalStateException:
    unknown vexJourneyMatrix; use baseline|all
connectedTaskFailed=true
failedTests=1
~~~

That proves the observability repair itself works on a real red instrumentation task.

## Unresolved uncertainty

The original PR #98 A001 failing testcase was not retained.

Therefore its exact failure cannot be reconstructed honestly.

The historical root cause remains:

~~~text
UNKNOWN
~~~

A future matching recurrence may provide enough retained evidence to explain it.

## Recurrence key

Treat a future event as potentially related when it has this shape:

~~~text
surface =
    vexVerifyDevice
    -> connectedDebugAndroidTest

symptom =
    intermittent instrumentation failure
    same/near-identical source may pass on retry

likely inspection areas =
    Journey synchronization
    Compose test scheduling
    device/runtime timing
    connected-test infrastructure
~~~

Do not assume relation solely from one matching word. Compare the retained failing testcase and
failure message first.

## Rightful owner

Operational current truth lives with:

~~~text
Test / Proof Foundation
docs/foundation/TEST-PROOF-ARCHITECTURE.md
docs/foundation/test-proof-integrity/
gradle/test-foundation.gradle.kts
~~~

VexLife DesignHistory owns the reusable continuity lesson, not this incident's operational status.

## Current consequence

The repository now preserves a distinction that did not previously exist durably:

~~~text
candidate currently passes
!=
foundation has never shown intermittent failure
~~~

This is intended to prevent technical debt from becoming habitual merely because the first warning
was cheap to ignore.

<!-- [VXG RealForever][integrity.test-proof.2026-10-16.001] -->
