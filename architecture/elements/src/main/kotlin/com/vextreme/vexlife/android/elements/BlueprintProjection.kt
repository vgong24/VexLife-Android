package com.vextreme.vexlife.android.elements

/**
 * Deterministic semantic projection of the registered UI Blueprint.
 *
 * This is deliberately serialization-neutral. The Elements foundation owns stable meaning and
 * relationships; an app/tooling boundary may encode this projection as JSON, logs, VexBoard import,
 * or another transport without making the portable element module depend on that transport.
 */
data class UIBlueprintProjection(
    val screens: List<UIBlueprintScreenProjection>,
    val shellElementRefs: List<String>,
    val elements: List<UIBlueprintElementProjection>,
)

data class UIBlueprintScreenProjection(
    val screenRef: String,
    val rootElementRef: String,
    val elementRefs: List<String>,
)

data class UIBlueprintElementProjection(
    val semanticRef: String,
    val kind: String,
    val styleRef: String,
    val ownerScreenRefOrNull: String?,
    val parentSemanticRefOrNull: String?,
    val childSemanticRefs: List<String>,
    val iconRefOrNull: String?,
    val textRoleOrNull: String?,
    val readOnlyOrNull: Boolean?,
    val defaultChildSemanticRefOrNull: String?,
    val intentOrNull: UIBlueprintIntentProjection?,
)

data class UIBlueprintIntentProjection(
    val kind: String,
    val screenRefs: List<String> = emptyList(),
    val elementRefs: List<String> = emptyList(),
    val actionRefs: List<String> = emptyList(),
    val children: List<UIBlueprintIntentProjection> = emptyList(),
)

/**
 * Projects the registry's current declared meaning without runtime navigation/presentation state.
 *
 * Lists with semantic ordering (children, intent Sequence) preserve declaration order. Top-level
 * screen/element collections are stable-ref sorted so transport diffs do not depend on map traversal.
 */
fun UIBlueprintRegistry.semanticProjection(): UIBlueprintProjection {
    val screens =
        allScreens
            .sortedBy { it.screenId }
            .map { screen ->
                UIBlueprintScreenProjection(
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
                UIBlueprintElementProjection(
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
                        when (element) {
                            is TextElement ->
                                element.role.name

                            is DynamicTextElement ->
                                element.role.name

                            else ->
                                null
                        },
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
                            ?.semanticProjection(),
                )
            }

    return UIBlueprintProjection(
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

private fun ElementIntent.semanticProjection(): UIBlueprintIntentProjection =
    when (this) {
        is ElementIntent.NavigateTo ->
            UIBlueprintIntentProjection(
                kind = "NAVIGATE_TO",
                screenRefs = listOf(screen.screenId),
            )

        ElementIntent.NavigateBack ->
            UIBlueprintIntentProjection(
                kind = "NAVIGATE_BACK",
            )

        is ElementIntent.Open ->
            UIBlueprintIntentProjection(
                kind = "OPEN",
                elementRefs = listOf(element.elementId),
            )

        is ElementIntent.Close ->
            UIBlueprintIntentProjection(
                kind = "CLOSE",
                elementRefs = listOf(element.elementId),
            )

        is ElementIntent.Toggle ->
            UIBlueprintIntentProjection(
                kind = "TOGGLE",
                elementRefs = listOf(element.elementId),
            )

        is ElementIntent.SwapTo ->
            UIBlueprintIntentProjection(
                kind = "SWAP_TO",
                elementRefs =
                    listOf(
                        container.elementId,
                        child.elementId,
                    ),
            )

        ElementIntent.LocalEffect ->
            UIBlueprintIntentProjection(
                kind = "LOCAL_EFFECT",
            )

        is ElementIntent.Invoke ->
            UIBlueprintIntentProjection(
                kind = "INVOKE",
                actionRefs = listOf(action.actionId),
            )

        is ElementIntent.Sequence ->
            UIBlueprintIntentProjection(
                kind = "SEQUENCE",
                children = intents.map { it.semanticProjection() },
            )
    }

// [VXG RealForever][UI-BLUEPRINT][SEMANTIC-PROJECTION]
