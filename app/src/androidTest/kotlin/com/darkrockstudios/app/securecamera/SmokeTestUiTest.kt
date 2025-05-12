package com.darkrockstudios.app.securecamera

import android.app.Application
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds


class SmokeTestUiTest {

	@get:Rule
	val permissionsRule = GrantPermissionRule.grant(
		android.Manifest.permission.POST_NOTIFICATIONS,
		android.Manifest.permission.ACCESS_FINE_LOCATION,
		android.Manifest.permission.CAMERA
	)

	@get:Rule
	val composeTestRule = createAndroidComposeRule<MainActivity>()

	@Test
	fun smokeTest() = runTest {
		composeTestRule.apply {
			onNodeWithText(str(R.string.intro_next)).performClick()
			onNodeWithText(str(R.string.intro_slide1_title)).assertIsDisplayed()

			onNodeWithText(str(R.string.intro_next)).performClick()
			onNodeWithText(str(R.string.intro_slide2_title)).assertIsDisplayed()

			onNodeWithText(str(R.string.intro_skip)).performClick()
			onNodeWithText(str(R.string.security_intro_supported_security_label)).assertIsDisplayed()

			onNodeWithText(str(R.string.intro_next)).performClick()
			onNodeWithText(str(R.string.pin_creation_title)).assertIsDisplayed()

			onNode(
				hasSetTextAction() and hasTextExactly(
					str(R.string.pin_creation_hint),
					includeEditableText = false
				)
			).performTextInput("3133734")

			onNode(
				hasSetTextAction() and hasTextExactly(
					str(R.string.pin_creation_confirm_hint),
					includeEditableText = false
				)
			).performTextInput("313373")

			onNodeWithText(str(R.string.pin_creation_button)).performClick()

			onNodeWithText(str(R.string.pin_creation_error)).assertIsDisplayed()

			onNode(
				hasSetTextAction() and hasTextExactly(
					str(R.string.pin_creation_hint),
					includeEditableText = false
				)
			).apply {
				performTextClearance()
				performTextInput("123456")
			}

			onNode(
				hasSetTextAction() and hasTextExactly(
					str(R.string.pin_creation_confirm_hint),
					includeEditableText = false
				)
			).apply {
				performTextClearance()
				performTextInput("123456")
			}

			onNodeWithText(str(R.string.pin_creation_button)).performClick()

			onNodeWithText(str(R.string.pin_creation_error_weak_pin)).assertIsDisplayed()

			onNode(
				hasSetTextAction() and hasTextExactly(
					str(R.string.pin_creation_hint),
					includeEditableText = false
				)
			).apply {
				performTextClearance()
				performTextInput("313373")
			}

			onNode(
				hasSetTextAction() and hasTextExactly(
					str(R.string.pin_creation_confirm_hint),
					includeEditableText = false
				)
			).apply {
				performTextClearance()
				performTextInput("313373")
			}

			onNodeWithText(str(R.string.pin_creation_button)).performClick()

			onNodeWithText(str(R.string.pin_creating_vault)).assertIsDisplayed()

			composeTestRule.waitUntil(
				timeoutMillis = 10.seconds.inWholeMilliseconds
			) {
				composeTestRule
					.onAllNodes(hasRole(Role.Button) and hasContentDescription(str(R.string.camera_shutter_button_desc)))
					.fetchSemanticsNodes().isNotEmpty()
			}

			onNode(
				hasRole(Role.Button) and hasContentDescription(str(R.string.camera_shutter_button_desc))
			).assertExists()
		}
	}

	fun hasRole(role: Role): SemanticsMatcher =
		SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

	private fun str(@StringRes id: Int): String = r.getString(id)
	private val r: Resources
		get() {
			val application = ApplicationProvider.getApplicationContext<Application?>()
			return application.resources
		}
}