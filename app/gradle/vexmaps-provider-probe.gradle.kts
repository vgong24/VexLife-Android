import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.Locale

/**
 * Feature-local VexMaps provider-reliability probe.
 *
 * This task is intentionally NOT part of vexVerifyLocal/vexVerifyDevice. It may contact live
 * external reality and consume provider budget. The task owns one bounded Stadia autocomplete
 * candidate probe until another real consumer earns promotion into shared Test / Proof Foundation.
 */
val vexMapsStadiaProbeFixtureFile =
    project.file(
        "src/providerProbe/resources/vexmaps-search-stadia-autocomplete-v2.json",
    )

val vexMapsStadiaProbeEvidenceFile =
    layout.buildDirectory.file(
        "vex/provider-reliability/stadia-autocomplete-v2.json",
    )

fun probeRequiredString(
    map: Map<*, *>,
    key: String,
): String =
    requireNotNull(
        map[key] as? String,
    ) {
        "VexMaps provider probe fixture is missing string '$key'"
    }
        .also {
            require(it.isNotBlank()) {
                "VexMaps provider probe fixture '$key' must not be blank"
            }
        }

fun probeRequiredInt(
    map: Map<*, *>,
    key: String,
): Int =
    requireNotNull(
        (map[key] as? Number)?.toInt(),
    ) {
        "VexMaps provider probe fixture is missing integer '$key'"
    }

fun probeRequiredDouble(
    map: Map<*, *>,
    key: String,
): Double =
    requireNotNull(
        (map[key] as? Number)?.toDouble(),
    ) {
        "VexMaps provider probe fixture is missing number '$key'"
    }

fun probeEncode(
    value: String,
): String =
    URLEncoder.encode(
        value,
        StandardCharsets.UTF_8,
    )

fun probeNormalize(
    value: String,
): String =
    value
        .lowercase(
            Locale.ROOT,
        )
        .filter(
            Char::isLetterOrDigit,
        )

tasks.register(
    "vexMapsProbeStadiaAutocompleteV2",
) {
    group = "verification"
    description =
        "Run the bounded live Stadia autocomplete candidate probe for VexMaps."

    inputs.file(
        vexMapsStadiaProbeFixtureFile,
    )
    outputs.file(
        vexMapsStadiaProbeEvidenceFile,
    )
    outputs.upToDateWhen {
        false
    }

    doLast {
        val apiKey =
            System.getenv(
                "VEXMAPS_STADIA_API_KEY",
            )
                ?.trim()
                ?.takeIf(
                    String::isNotEmpty,
                )
                ?: throw GradleException(
                    "VEXMAPS_STADIA_API_KEY is required for the opt-in live provider probe. " +
                        "The key is read from the host environment only and is never written to evidence.",
                )

        @Suppress("UNCHECKED_CAST")
        val root =
            JsonSlurper()
                .parse(
                    vexMapsStadiaProbeFixtureFile,
                ) as Map<String, Any?>

        val schemaRef =
            probeRequiredString(
                root,
                "schemaRef",
            )
        require(
            schemaRef ==
                "vexmaps.search-provider-probe-fixtures/v1",
        ) {
            "Unsupported VexMaps provider probe fixture schema: $schemaRef"
        }

        val providerRef =
            probeRequiredString(
                root,
                "providerRef",
            )
        require(
            providerRef ==
                "provider.stadia.autocomplete.v2",
        ) {
            "Unexpected VexMaps provider probe provider: $providerRef"
        }

        val fixtureSetRef =
            probeRequiredString(
                root,
                "fixtureSetRef",
            )
        val maxRequestsPerRun =
            probeRequiredInt(
                root,
                "maxRequestsPerRun",
            )
        val resultLimit =
            probeRequiredInt(
                root,
                "resultLimit",
            )

        require(
            maxRequestsPerRun in
                1..10,
        ) {
            "VexMaps provider probe request budget must be 1..10"
        }
        require(
            resultLimit in
                1..20,
        ) {
            "VexMaps provider probe result limit must be 1..20"
        }

        val fixtures =
            requireNotNull(
                root["fixtures"] as?
                    List<*>,
            ) {
                "VexMaps provider probe fixture list is missing"
            }

        require(
            fixtures.isNotEmpty(),
        ) {
            "VexMaps provider probe fixture list must not be empty"
        }
        require(
            fixtures.size <=
                maxRequestsPerRun,
        ) {
            "VexMaps provider probe fixture count exceeds the declared request budget"
        }

        val httpClient =
            HttpClient
                .newBuilder()
                .connectTimeout(
                    Duration.ofSeconds(
                        10,
                    ),
                )
                .followRedirects(
                    HttpClient.Redirect.NORMAL,
                )
                .build()

        val results =
            mutableListOf<
                Map<String, Any?>
            >()

        fixtures.forEach { rawFixture ->
            @Suppress("UNCHECKED_CAST")
            val fixture =
                rawFixture as?
                    Map<String, Any?>
                    ?: error(
                        "VexMaps provider probe fixture must be an object",
                    )

            val fixtureRef =
                probeRequiredString(
                    fixture,
                    "fixtureRef",
                )
            val capabilityClass =
                probeRequiredString(
                    fixture,
                    "capabilityClass",
                )
            val queryText =
                probeRequiredString(
                    fixture,
                    "text",
                )
            val boundaryCountry =
                probeRequiredString(
                    fixture,
                    "boundaryCountry",
                )

            @Suppress("UNCHECKED_CAST")
            val layers =
                (
                    fixture["layers"] as?
                        List<*>
                )
                    ?.map { layer ->
                        require(
                            layer is String &&
                                layer.isNotBlank(),
                        ) {
                            "VexMaps provider probe layers must be non-blank strings"
                        }
                        layer
                    }
                    .orEmpty()

            @Suppress("UNCHECKED_CAST")
            val focus =
                requireNotNull(
                    fixture["focus"] as?
                        Map<String, Any?>,
                ) {
                    "VexMaps provider probe fixture '$fixtureRef' needs a public focus"
                }

            val focusRef =
                probeRequiredString(
                    focus,
                    "focusRef",
                )
            val focusLatitude =
                probeRequiredDouble(
                    focus,
                    "latitude",
                )
            val focusLongitude =
                probeRequiredDouble(
                    focus,
                    "longitude",
                )

            require(
                focusLatitude in
                    -90.0..90.0 &&
                    focusLongitude in
                    -180.0..180.0,
            ) {
                "VexMaps provider probe focus is outside valid coordinate bounds: $focusRef"
            }

            @Suppress("UNCHECKED_CAST")
            val expected =
                requireNotNull(
                    fixture["expect"] as?
                        Map<String, Any?>,
                ) {
                    "VexMaps provider probe fixture '$fixtureRef' needs an expectation"
                }

            val minimumResults =
                probeRequiredInt(
                    expected,
                    "minResults",
                )
            val expectedNameToken =
                (
                    expected[
                        "nameContainsNormalized"
                    ] as?
                        String
                )
                    ?.trim()
                    ?.takeIf(
                        String::isNotEmpty,
                    )

            val queryParameters =
                buildList {
                    add(
                        "text=" +
                            probeEncode(
                                queryText,
                            ),
                    )
                    add(
                        "api_key=" +
                            probeEncode(
                                apiKey,
                            ),
                    )
                    add(
                        "lang=en",
                    )
                    add(
                        "size=$resultLimit",
                    )
                    add(
                        "boundary.country=" +
                            probeEncode(
                                boundaryCountry,
                            ),
                    )
                    add(
                        "focus.point.lat=$focusLatitude",
                    )
                    add(
                        "focus.point.lon=$focusLongitude",
                    )
                    if (
                        layers.isNotEmpty()
                    ) {
                        add(
                            "layers=" +
                                probeEncode(
                                    layers.joinToString(
                                        separator = ",",
                                    ),
                                ),
                        )
                    }
                }

            val uri =
                URI.create(
                    "https://api.stadiamaps.com/geocoding/v2/autocomplete?" +
                        queryParameters.joinToString(
                            separator = "&",
                        ),
                )

            val startedAt =
                System.nanoTime()

            var httpStatus:
                Int? =
                null
            var resultNames =
                emptyList<String>()
            var resultLayers =
                emptyList<String>()
            var failureMessage:
                String? =
                null

            try {
                val response =
                    httpClient.send(
                        HttpRequest
                            .newBuilder(
                                uri,
                            )
                            .timeout(
                                Duration.ofSeconds(
                                    15,
                                ),
                            )
                            .header(
                                "Accept",
                                "application/json",
                            )
                            .GET()
                            .build(),
                        HttpResponse
                            .BodyHandlers
                            .ofString(),
                    )

                httpStatus =
                    response.statusCode()

                if (
                    httpStatus in
                    200..299
                ) {
                    @Suppress("UNCHECKED_CAST")
                    val payload =
                        JsonSlurper()
                            .parseText(
                                response.body(),
                            ) as?
                            Map<String, Any?>
                            ?: error(
                                "Stadia autocomplete response was not a JSON object",
                            )

                    val features =
                        (
                            payload["features"] as?
                                List<*>
                        )
                            .orEmpty()

                    val properties =
                        features.mapNotNull { feature ->
                            @Suppress("UNCHECKED_CAST")
                            (
                                feature as?
                                    Map<String, Any?>
                            )
                                ?.get(
                                    "properties",
                                ) as?
                                Map<*, *>
                        }

                    resultNames =
                        properties
                            .mapNotNull { property ->
                                property["name"] as?
                                    String
                            }
                            .take(
                                resultLimit,
                            )

                    resultLayers =
                        properties
                            .mapNotNull { property ->
                                property["layer"] as?
                                    String
                            }
                            .take(
                                resultLimit,
                            )
                } else {
                    failureMessage =
                        "HTTP_$httpStatus"
                }
            } catch (
                failure:
                    Exception,
            ) {
                failureMessage =
                    failure::class
                        .simpleName
                        ?.takeIf(
                            String::isNotBlank,
                        )
                        ?: "PROBE_EXCEPTION"
            }

            val elapsedMillis =
                (
                    System.nanoTime() -
                        startedAt
                ) /
                    1_000_000L

            val tokenMatched =
                expectedNameToken == null ||
                    resultNames.any { name ->
                        probeNormalize(
                            name,
                        ).contains(
                            probeNormalize(
                                expectedNameToken,
                            ),
                        )
                    }

            val accepted =
                httpStatus in
                    200..299 &&
                    resultNames.size >=
                    minimumResults &&
                    tokenMatched

            results +=
                linkedMapOf(
                    "fixtureRef" to
                        fixtureRef,
                    "capabilityClass" to
                        capabilityClass,
                    "query" to
                        queryText,
                    "focusRef" to
                        focusRef,
                    "httpStatus" to
                        httpStatus,
                    "elapsedMillis" to
                        elapsedMillis,
                    "resultCount" to
                        resultNames.size,
                    "topNames" to
                        resultNames.take(5),
                    "topLayers" to
                        resultLayers.take(5),
                    "expectedMinimumResults" to
                        minimumResults,
                    "expectedNameContainsNormalized" to
                        expectedNameToken,
                    "nameExpectationMatched" to
                        tokenMatched,
                    "accepted" to
                        accepted,
                    "failure" to
                        failureMessage,
                )
        }

        val disposition =
            if (
                results.all { result ->
                    result["accepted"] ==
                        true
                }
            ) {
                "PASS"
            } else {
                "FAIL"
            }

        val evidence =
            linkedMapOf(
                "schemaRef" to
                    "vexmaps.provider-reliability-evidence/v1",
                "providerRef" to
                    providerRef,
                "fixtureSetRef" to
                    fixtureSetRef,
                "ranAtUtc" to
                    Instant.now()
                        .toString(),
                "requestBudget" to
                    maxRequestsPerRun,
                "requestsUsed" to
                    results.size,
                "resultLimit" to
                    resultLimit,
                "apiKeyPersisted" to
                    false,
                "privateCoordinatesPersisted" to
                    false,
                "results" to
                    results,
                "disposition" to
                    disposition,
            )

        val output =
            vexMapsStadiaProbeEvidenceFile
                .get()
                .asFile
        output.parentFile.mkdirs()
        output.writeText(
            JsonOutput.prettyPrint(
                JsonOutput.toJson(
                    evidence,
                ),
            ) +
                "\n",
        )

        logger.lifecycle(
            "VexMaps Stadia autocomplete probe: $disposition " +
                "(requests=\${results.size}, evidence=\${output.relativeTo(project.projectDir)})",
        )

        if (
            disposition !=
            "PASS"
        ) {
            throw GradleException(
                "VexMaps Stadia autocomplete probe did not satisfy the fixture contract. " +
                    "Evidence: \${output.absolutePath}",
            )
        }
    }
}
