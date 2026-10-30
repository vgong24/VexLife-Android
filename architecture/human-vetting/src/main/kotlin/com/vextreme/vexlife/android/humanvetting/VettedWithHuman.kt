package com.vextreme.vexlife.android.humanvetting

/**
 * Records an architecture boundary that Victor explicitly reviewed and acknowledged with the AI.
 *
 * HUMAN PROVENANCE RULE:
 *
 * - This annotation records a human decision that already happened. It never creates approval.
 * - An AI MUST NOT add, copy, broaden, preserve, or synthesize this annotation for new or changed
 *   architecture without first explaining the architectural change to Victor and receiving his
 *   explicit acknowledgement of that exact change.
 * - Tests, builds, source proof, Independent Assurance, merge status, PR text, documentation, or
 *   words such as "foundation", "accepted", "canonical", "approved", or "stable" are NOT substitutes
 *   for Victor's acknowledgement.
 * - Copying an existing approval string or timestamp onto another declaration is a false provenance
 *   claim.
 * - A semantic change to an annotated declaration reopens human vetting. Do not carry the marker
 *   forward merely because the class name stayed the same.
 * - The annotation covers only the semantic contract documented directly on the annotated
 *   declaration. Nested or neighboring declarations are NOT vetted unless they carry their own
 *   [VettedWithHuman] marker.
 * - Architecture is not only nouns. Functions, properties, mappings, handoff seams, arrival seams,
 *   and concrete examples can be load-bearing architecture. When Victor vets a relationship, mark
 *   the declarations that actually carry that relationship rather than blessing only its endpoint
 *   class names.
 * - A class-level marker MUST NOT be used to hide a changed unmarked connection inside that class.
 *   If an annotated relationship is rewired through a new semantic owner, adapter, state holder,
 *   identity, or arrival path, that architectural change must be surfaced to Victor first.
 *
 * This is intentionally source-retained metadata. It is a warning and provenance boundary for
 * humans and AI source authors, not runtime behavior and not a test oracle.
 */
@MustBeDocumented
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
)
@Retention(AnnotationRetention.SOURCE)
annotation class VettedWithHuman(
    val declarationName: String,
    val vettedAt: String,
    val approval: String,
)
