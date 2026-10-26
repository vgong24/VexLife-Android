package com.vextreme.vexlife.android.elements

/**
 * Deterministic projection of the registered Android UI realization.
 *
 * This is deliberately serialization-neutral. The Elements foundation owns Android-side registered
 * meaning and relationships; an app/tooling boundary may encode this projection as JSON, logs,
 * VexBoard import, or another transport without making the portable element module depend on that
 * transport. Projection does not make Android the portable Origin owner.
 */
data class UIRealizationProjection(
    val screens: List<UIScreenRealization>,
    val shellElementRefs: List<String>,
    val elements: List<UIElementRealization>,
)

data class UIScreenRealization(
    val screenRef: String,
    val rootElementRef: String,
    val elementRefs: List<String>,
)

data class UIElementRealization(
    val semanticRef: String,
    val kind: String,
    val styleRef: String,
    val ownerScreenRefOrNull: String?,
    val parentSemanticRefOrNull: String?,
    val childSemanticRefs: List<String>,
    val iconRefOrNull: String?,
    val textRoleOrNull: String?,
    val textResourceIdOrNull: Int?,
    val readOnlyOrNull: Boolean?,
    val defaultChildSemanticRefOrNull: String?,
    val intentOrNull: UIIntentRealization?,
)

data class UIIntentRealization(
    val kind: String,
    val screenRefs: List<String> = emptyList(),
    val elementRefs: List<String> = emptyList(),
    val actionRefs: List<String> = emptyList(),
    val children: List<UIIntentRealization> = emptyList(),
)

/**
 * Projects the registry's current target-realization meaning without runtime navigation/presentation state.
 *
 * Lists with semantic ordering (children, intent Sequence) preserve declaration order. Top-level
 * screen/element collections are stable-ref sorted so transport diffs do not depend on map traversal.
 */
fun UIBlueprintRegistry.realizationProjection(): UIRealizationProjection {
    val screens =
        allScreens
            .sortedBy { it.screenId }
            .map { screen ->
                UIScreenRealization(
                    screenRef = screen.screenId,
                    rootElementRef = screenRoot(screen).element.elementId,
                    elementRefs =
                        elementsForScreen(screen)
                            .map { it.elementId }
                            .sorted(),
                )
            }

    val elements =
        allElements
            .sortedBy { it.elementId }
            .map { element ->
                UIElementRealization(
                    semanticRef = element.elementId,
                    kind = element.kind,
                    styleRef = styleOf(element).value,
                    ownerScreenRefOrNull =
                        ownerScreenOf(element)
                            ?.screenId,
                    parentSemanticRefOrNull =
                        parentOf(element)
                            ?.elementId,
                    childSemanticRefs =
                        childrenOf(element)
                            .map { it.elementId },
                    iconRefOrNull =
                        when (element) {
                            is ButtonElement ->
                                element.icon
                                    ?.projectionRef()

                            is FabElement ->
                                element.icon
                                    .projectionRef()

                            else ->
                                null
                        },
                    textRoleOrNull =
                        (element as? TextElement)
                            ?.role
                            ?.name,
                    textResourceIdOrNull =
                        (element as? TextElement)
                            ?.textRes,
                    readOnlyOrNull =
                        (element as? TextFieldElement)
                            ?.readOnly,
                    defaultChildSemanticRefOrNull =
                        (element as? SwappableContainerElement)
                            ?.defaultChild
                            ?.elementId,
                    intentOrNull =
                        (element as? ActionElement)
                            ?.intent
                            ?.realizationProjection(),
                )
            }

    return UIRealizationProjection(
        screens = screens,
        shellElementRefs =
            shellElements.map { it.elementId },
        elements = elements,
    )
}

private fun ElementIcon.projectionRef(): String =
    "icon." +
        name
            .replace(Regex("([a-z0-9])([A-Z])"), "$1-$2")
            .lowercase()

private fun ElementIntent.realizationProjection(): UIIntentRealization =
    when (this) {
        is ElementIntent.NavigateTo ->
            UIIntentRealization(
                kind = "NAVIGATE_TO",
                screenRefs = listOf(screen.screenId),
            )

        ElementIntent.NavigateBack ->
            UIIntentRealization(
                kind = "NAVIGATE_BACK",
            )

        is ElementIntent.Open ->
            UIIntentRealization(
                kind = "OPEN",
                elementRefs = listOf(element.elementId),
            )

        is ElementIntent.Close ->
            UIIntentRealization(
                kind = "CLOSE",
                elementRefs = listOf(element.elementId),
            )

        is ElementIntent.Toggle ->
            UIIntentRealization(
                kind = "TOGGLE",
                elementRefs = listOf(element.elementId),
            )

        is ElementIntent.SwapTo ->
            UIIntentRealization(
                kind = "SWAP_TO",
                elementRefs =
                    listOf(
                        container.elementId,
                        child.elementId,
                    ),
            )

        ElementIntent.LocalEffect ->
            UIIntentRealization(
                kind = "LOCAL_EFFECT",
            )

        is ElementIntent.Invoke ->
            UIIntentRealization(
                kind = "INVOKE",
                actionRefs = listOf(action.actionId),
            )

        is ElementIntent.Sequence ->
            UIIntentRealization(
                kind = "SEQUENCE",
                children = intents.map { it.realizationProjection() },
            )
    }

// [VXG RealForever][UI-REALIZATION][ORIGIN-LINEAGE][PROJECTION]
