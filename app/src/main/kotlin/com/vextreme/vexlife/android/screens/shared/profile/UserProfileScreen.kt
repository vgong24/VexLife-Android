package com.vextreme.vexlife.android.screens.shared.profile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.preview.VexPreviewRepositories
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.data.userprofile.UserProfile
import com.vextreme.vexlife.android.data.userprofile.UserProfileFixture
import com.vextreme.vexlife.android.data.userprofile.UserProfileRefs
import com.vextreme.vexlife.android.data.userprofile.UserProfileRepository
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.ElementIcon
import com.vextreme.vexlife.android.elements.FormSubmission
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexElementTree
import com.vextreme.vexlife.android.elements.invoke
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.screens.AppScreenBlueprint
import com.vextreme.vexlife.android.screens.ScreenActionContext
import com.vextreme.vexlife.android.runtime.AppActions
import com.vextreme.vexlife.android.runtime.appAction
import com.vextreme.vexlife.android.runtime.VexRuntime
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * First database-backed User Profile screen.
 *
 * The screen is intentionally small: one stable local profile ref, one persisted display name, one
 * editable draft, and Save. Its purpose is to prove the UI -> repository -> Room -> observable UI
 * loop before VexHome/device/account relationship semantics are designed.
 *
 * Future VexHome/VexStream/device links do NOT belong in this screen's profile row. A future User Hub
 * may compose profile + linked-system state from separate owners.
 */
object UserProfileScreen : AppScreenBlueprint(
    screenKey = "user-profile",
    titleRes = R.string.user_profile_title,
) {
    val PersistedName = textFieldRef(
        id = "body.profile.persisted-name",
        labelRes = R.string.user_profile_persisted_name_label,
        readOnly = true,
    )

    val DisplayName = textFieldRef(
        id = "body.profile.display-name",
        labelRes = R.string.user_profile_name_label,
    )

    val Save = buttonRef(
        id = "body.profile.actions.save",
        labelRes = R.string.user_profile_save,
        icon = ElementIcon.Save,
        intent = invoke(AppActions.SaveUserProfile),
    )

    override val content = layout {
        scrollableColumn("body") {
            card("profile") {
                text(
                    id = "intro",
                    textRes = R.string.user_profile_intro,
                    role = TextRole.Supporting,
                )
                textField(PersistedName)
                textField(DisplayName)
                button(Save)
            }
        }
    }

    internal override fun appActionRegistrations(
        context: ScreenActionContext,
    ): AppActionRegistrations =
        userProfileActionRegistrations(
            repository = context.repositories.userProfile,
            scope = context.scope,
        )
}

/**
 * Stateful renderer for [UserProfileScreen].
 *
 * The read-only field observes repository truth. The editable field is a local unsaved draft.
 * Save creates one immutable [FormSubmission] and hands it to VexRuntime. Draft keystrokes never
 * become runtime actions/log records. The app action owner performs persistence; Room's Flow then
 * updates the persisted field so the database round-trip is visible on-screen.
 *
 * This keeps the screen responsible for draft presentation state while persistence/operation ownership
 * remains outside the screen.
 */
@Composable
fun UserProfileScreenContent(
    runtime: VexRuntime,
    repository: UserProfileRepository,
) {
    val profile by repository
        .observe(UserProfileRefs.local)
        .collectAsState(initial = null)

    var draftName by rememberSaveable { mutableStateOf("") }
    var draftInitialized by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(profile?.displayName) {
        if (!draftInitialized && profile != null) {
            draftName = profile?.displayName.orEmpty()
            draftInitialized = true
        }
    }

    /*
     * Only Save crosses the form boundary. Typing remains local draft state and is deliberately not
     * routed through VexRuntime/VexLogger.
     */
    val screenActionHandler = ElementActionHandler { element ->
        if (element === UserProfileScreen.Save) {
            runtime.submit(
                element = element,
                submission = FormSubmission.of(
                    UserProfileScreen.DisplayName to draftName,
                ),
            )
        } else {
            runtime.activate(element)
        }
    }

    VexElementTree(
        placement = UserProfileScreen.content,
        registry = runtime.registry,
        handler = screenActionHandler,
        modifier = Modifier.fillMaxSize(),
        textFieldValue = { element ->
            when (element) {
                UserProfileScreen.PersistedName -> profile?.displayName.orEmpty()
                UserProfileScreen.DisplayName -> draftName
                else -> ""
            }
        },
        onTextFieldValueChange = { element, value ->
            if (element === UserProfileScreen.DisplayName) {
                draftName = value
                draftInitialized = true
            }
        },
        selectedChild = runtime.presentation::selected,
    )
}

/**
 * Persistence effect for UserProfileScreen's Save action.
 *
 * The Screen owns how its submitted fields become a profile mutation. The app root supplies the
 * longer-lived repository/scope, while AppActionRegistrations guarantees this ActionKey has only one owner.
 * Draft typing never reaches this registration; only the immutable Save submission does.
 */
internal fun userProfileActionRegistrations(
    repository: UserProfileRepository,
    scope: CoroutineScope,
    now: () -> Instant = { Instant.now() },
): AppActionRegistrations =
    AppActionRegistrations.of(
        appAction(
            action = AppActions.SaveUserProfile,
            ownerRef = UserProfileScreen.screenId,
        ) { request ->
            val requestedName = request.submission
                ?.value(UserProfileScreen.DisplayName)
                ?.trim()
                .orEmpty()

            if (requestedName.isNotEmpty()) {
                scope.launch {
                    repository.upsert(
                        UserProfile(
                            profileRef = UserProfileRefs.local,
                            displayName = requestedName,
                            updatedAt = now(),
                        ),
                    )
                }
            }
        },
    )

@VexScreenPreviews
@Composable
private fun UserProfileScreenPreview() {
    VexPreviewHost(
        id = "user-profile-existing",
        startScreen = UserProfileScreen,
        repositoriesFactory = {
            VexPreviewRepositories.withUserProfile(
                UserProfileFixture.build {
                    displayName("Victor")
                },
            )
        },
    ) { context ->
        UserProfileScreenContent(
            runtime = context.runtime,
            repository = context.repositories.userProfile,
        )
    }
}

@VexScreenPreviews
@Composable
private fun UserProfileFirstSavePreview() {
    VexPreviewHost(
        id = "user-profile-empty",
        startScreen = UserProfileScreen,
    ) { context ->
        UserProfileScreenContent(
            runtime = context.runtime,
            repository = context.repositories.userProfile,
        )
    }
}
