package com.vextreme.vexlife.android.testing.featurewalk

private val featureRefPattern =
    Regex("feature\\.[a-z0-9][a-z0-9.-]*")
private val keyPattern =
    Regex("[a-z0-9][a-z0-9-]*")
private val walkRefPattern =
    Regex(".+/W[0-9]{2}")
private val caseRefPattern =
    Regex(".+/W[0-9]{2}/C[0-9]{2}")
private val checkpointRefPattern =
    Regex(".+/W[0-9]{2}/C[0-9]{2}/P[0-9]{2}")

/**
 * Whether a Case/Checkpoint belongs to current product completion or a named future frontier.
 *
 * A hold is modeled separately because a required thing can be real and currently blocked.
 */
enum class VexFeatureWalkCommitment {
    REQUIRED,
    FUTURE,
}

data class VexFeatureWalkHold(
    val reason: String,
    val releasePredicate: String,
) {
    init {
        require(reason.isNotBlank()) {
            "feature-walk hold reason must not be blank"
        }
        require(releasePredicate.isNotBlank()) {
            "feature-walk hold release predicate must not be blank"
        }
    }
}

data class VexFeatureWalkCheckpoint(
    val ref: String,
    val key: String,
    val title: String,
    val commitment: VexFeatureWalkCommitment =
        VexFeatureWalkCommitment.REQUIRED,
    val hold: VexFeatureWalkHold? = null,
) {
    init {
        require(checkpointRefPattern.matches(ref)) {
            "feature-walk checkpoint ref must end in /Wnn/Cnn/Pnn: $ref"
        }
        require(keyPattern.matches(key)) {
            "feature-walk checkpoint key must be lowercase kebab-case: $key"
        }
        require(title.isNotBlank()) {
            "feature-walk checkpoint title must not be blank"
        }
    }
}

data class VexFeatureWalkCase(
    val ref: String,
    val key: String,
    val title: String,
    val commitment: VexFeatureWalkCommitment =
        VexFeatureWalkCommitment.REQUIRED,
    val checkpoints: List<VexFeatureWalkCheckpoint>,
) {
    init {
        require(caseRefPattern.matches(ref)) {
            "feature-walk case ref must end in /Wnn/Cnn: $ref"
        }
        require(keyPattern.matches(key)) {
            "feature-walk case key must be lowercase kebab-case: $key"
        }
        require(title.isNotBlank()) {
            "feature-walk case title must not be blank"
        }
        require(checkpoints.isNotEmpty()) {
            "feature-walk case must declare checkpoints: $ref"
        }
        require(
            checkpoints.all { checkpoint ->
                checkpoint.ref.substringBeforeLast('/') == ref
            },
        ) {
            "feature-walk checkpoints must be direct children of their case: $ref"
        }
        require(
            checkpoints.map { it.ref }.distinct().size ==
                checkpoints.size,
        ) {
            "feature-walk checkpoint refs must be unique inside case: $ref"
        }
        require(
            checkpoints.map { it.key }.distinct().size ==
                checkpoints.size,
        ) {
            "feature-walk checkpoint keys must be unique inside case: $ref"
        }
    }

    val requiredHolds: List<VexFeatureWalkCheckpoint>
        get() =
            checkpoints.filter { checkpoint ->
                checkpoint.commitment ==
                    VexFeatureWalkCommitment.REQUIRED &&
                    checkpoint.hold != null
            }

    /**
     * A current required Case earns Journey binding only after every required checkpoint is unheld.
     *
     * This does not claim the Journey passed. It states that the human path is executable enough to
     * belong in the Journey catalog.
     */
    val journeyExpectedNow: Boolean
        get() =
            commitment ==
                VexFeatureWalkCommitment.REQUIRED &&
                requiredHolds.isEmpty()
}

data class VexFeatureWalk(
    val ref: String,
    val key: String,
    val title: String,
    val cases: List<VexFeatureWalkCase>,
) {
    init {
        require(walkRefPattern.matches(ref)) {
            "feature-walk ref must end in /Wnn: $ref"
        }
        require(keyPattern.matches(key)) {
            "feature-walk key must be lowercase kebab-case: $key"
        }
        require(title.isNotBlank()) {
            "feature-walk title must not be blank"
        }
        require(cases.isNotEmpty()) {
            "feature walk must declare cases: $ref"
        }
        require(
            cases.all { case ->
                case.ref.substringBeforeLast('/') == ref
            },
        ) {
            "feature-walk cases must be direct children of their walk: $ref"
        }
        require(
            cases.map { it.ref }.distinct().size ==
                cases.size,
        ) {
            "feature-walk case refs must be unique inside walk: $ref"
        }
        require(
            cases.map { it.key }.distinct().size ==
                cases.size,
        ) {
            "feature-walk case keys must be unique inside walk: $ref"
        }
    }
}

data class VexFeatureWalkFeature(
    val featureRef: String,
    val currentName: String,
    val walks: List<VexFeatureWalk>,
) {
    init {
        require(featureRefPattern.matches(featureRef)) {
            "feature ref must be lowercase dot-separated: $featureRef"
        }
        require(currentName.isNotBlank()) {
            "feature name must not be blank"
        }
        require(walks.isNotEmpty()) {
            "feature must declare at least one walk: $featureRef"
        }
        require(
            walks.all { walk ->
                walk.ref.substringBeforeLast('/') == featureRef
            },
        ) {
            "feature walks must be direct children of their feature: $featureRef"
        }
        require(
            walks.map { it.ref }.distinct().size ==
                walks.size,
        ) {
            "feature-walk refs must be unique inside feature: $featureRef"
        }
        require(
            walks.map { it.key }.distinct().size ==
                walks.size,
        ) {
            "feature-walk keys must be unique inside feature: $featureRef"
        }
    }
}

// [VXG RealForever]
