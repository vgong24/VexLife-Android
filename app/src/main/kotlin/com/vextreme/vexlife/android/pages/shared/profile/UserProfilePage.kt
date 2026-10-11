package com.vextreme.vexlife.android.pages.shared.profile

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
import com.vextreme.vexlife.android.data.userprofile.UserProfileRefs
import com.vextreme.vexlife.android.data.userprofile.UserProfileRepository
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.ElementIcon
import com.vextreme.vexlife.android.elements.FormSubmission
import com.vextreme.vexlife.android.elements.PageBlueprint
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexElementTree
import com.vextreme.vexlife.android.elements.invoke
import com.vextreme.vexlife.android.runtime.AppActions
import com.vextreme.vexlife.android.runtime.VexRuntime

/**
 * First database-backed User Profile page.
 *
 * The page is intentionally small: one stable local profile ref, one persisted display name, one
 * editable draft, and Save. Its purpose is to prove the UI -> repository -> Room -> observable UI
 * loop before VexHome/device/account relationship semantics are designed.
 *
 * Future VexHome/VexStream/device links do NOT belong in this page's profile row. A future User Hub
 * may compose profile + linked-system state from separate owners.
 */
object UserProfilePage : PageBlueprint(
    pageId = "user-profile",
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
}

/**
 * Stateful renderer for [UserProfilePage].
 *
 * The read-only field observes repository truth. The editable field is a local unsaved draft.
 * Save creates one immutable [FormSubmission] and hands it to VexRuntime. Draft keystrokes never
 * become runtime actions/log records. The app action owner performs persistence; Room's Flow then
 * updates the persisted field so the database round-trip is visible on-screen.
 *
 * This keeps the page responsible for draft presentation state while persistence/operation ownership
 * remains outside the page.
 */
@Composable
fun UserProfilePageContent(
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
    val pageActionHandler = ElementActionHandler { element ->
        if (element === UserProfilePage.Save) {
            runtime.submit(
                element = element,
                submission = FormSubmission.of(
                    UserProfilePage.DisplayName to draftName,
                ),
            )
        } else {
            runtime.activate(element)
        }
    }

    VexElementTree(
        placement = UserProfilePage.content,
        registry = runtime.registry,
        handler = pageActionHandler,
        modifier = Modifier.fillMaxSize(),
        textFieldValue = { element ->
            when (element) {
                UserProfilePage.PersistedName -> profile?.displayName.orEmpty()
                UserProfilePage.DisplayName -> draftName
                else -> ""
            }
        },
        onTextFieldValueChange = { element, value ->
            if (element === UserProfilePage.DisplayName) {
                draftName = value
                draftInitialized = true
            }
        },
        selectedChild = runtime.presentation::selected,
    )
}
