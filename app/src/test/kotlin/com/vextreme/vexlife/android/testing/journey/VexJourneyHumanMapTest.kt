package com.vextreme.vexlife.android.testing.journey

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Protects the human/IDE journey map from drifting away from the executable catalog.
 *
 * A feature author should be able to discover every runnable journey by typing "Journeys." without
 * reading the whole catalog or knowing raw shard IDs. Conversely, a typed human path must never
 * point at a journey that is absent from the executable catalog.
 */
class VexJourneyHumanMapTest {
    @Test
    fun everyExecutableJourneyHasExactlyOneTypedHumanPath() {
        val typedPaths = collectTypedJourneyPaths()

        assertTrue(
            "Journeys should expose at least one typed path",
            typedPaths.isNotEmpty(),
        )

        val resolved = typedPaths.map { (label, path) ->
            val journey =
                try {
                    journeyFor(path)
                } catch (failure: Throwable) {
                    throw AssertionError(
                        "typed journey path is not engine-bound: Journeys.$label",
                        failure,
                    )
                }
            label to journey
        }

        val duplicateJourneyBindings = resolved
            .groupBy { (_, journey) -> journey }
            .filterValues { bindings -> bindings.size > 1 }

        assertTrue(
            "one executable journey must not have multiple typed human paths: " +
                duplicateJourneyBindings
                    .mapValues { (_, bindings) -> bindings.map { it.first } },
            duplicateJourneyBindings.isEmpty(),
        )

        assertEquals(
            "typed Journeys.* paths must cover the executable journey catalog exactly",
            VexJourneyCatalog.all.toSet(),
            resolved.map { (_, journey) -> journey }.toSet(),
        )

        assertEquals(
            "typed path count must match executable journey count",
            VexJourneyCatalog.all.size,
            resolved.size,
        )
    }
}

/**
 * Reflection is intentionally test-only. Production keeps Journeys/JourneyPath free of registry
 * plumbing so IDE completion remains a small human-facing map.
 */
private fun collectTypedJourneyPaths(): List<Pair<String, JourneyPath>> =
    Journeys::class.java.declaredClasses
        .flatMap { domainClass ->
            val instanceField =
                domainClass.declaredFields.firstOrNull { field ->
                    field.name == "INSTANCE"
                }
                    ?: return@flatMap emptyList()

            check(instanceField.trySetAccessible()) {
                "cannot inspect Journeys domain ${domainClass.simpleName}"
            }
            val instance = instanceField.get(null)

            domainClass.declaredMethods
                .filter { method ->
                    method.parameterCount == 0 &&
                        method.returnType == JourneyPath::class.java
                }
                .map { method ->
                    check(method.trySetAccessible()) {
                        "cannot inspect ${domainClass.simpleName}.${method.name}"
                    }

                    val path = method.invoke(instance) as JourneyPath
                    val propertyName = method.name
                        .removePrefix("get")
                        .replaceFirstChar { first -> first.lowercase() }

                    "${domainClass.simpleName}.$propertyName" to path
                }
        }
        .sortedBy { (label, _) -> label }
