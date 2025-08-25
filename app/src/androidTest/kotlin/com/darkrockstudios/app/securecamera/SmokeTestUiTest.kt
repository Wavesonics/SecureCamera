package com.darkrockstudios.app.securecamera

import android.Manifest
import android.app.Application
import android.content.res.Resources
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


class SmokeTestUiTest {

	@get:Rule
	val permissionsRule: GrantPermissionRule =
		if (Build.VERSION.SDK_INT >= 33) {
			GrantPermissionRule.grant(
				Manifest.permission.CAMERA,
				Manifest.permission.ACCESS_FINE_LOCATION,
				Manifest.permission.POST_NOTIFICATIONS,
			)
		} else {
			GrantPermissionRule.grant(
				Manifest.permission.CAMERA,
				Manifest.permission.ACCESS_FINE_LOCATION,
			)
		}

	@get:Rule
	val composeTestRule = createAndroidComposeRule<MainActivity>()

	@Test
	fun smokeTest() {
		composeTestRule.apply {
			onNodeWithText(str(R.string.intro_next)).performClick()
			onNodeWithText(str(R.string.intro_slide1_title)).assertIsDisplayed()

			onNodeWithText(str(R.string.intro_next)).performClick()
			onNodeWithText(str(R.string.intro_slide2_title)).assertIsDisplayed()

			onNodeWithText(str(R.string.intro_skip)).performClick()
			onNodeWithText(str(R.string.security_intro_supported_security_label)).assertIsDisplayed()

			onNodeWithText(str(R.string.intro_next)).performClick()
			onNodeWithText(str(R.string.pin_creation_title)).assertIsDisplayed()

			setPinFields("3133734", "313373")
			onNodeWithText(str(R.string.pin_creation_button)).performClick()
			waitForTextEitherTree(R.string.pin_creation_error)

			setPinFields("123456", "123456")
			onNodeWithText(str(R.string.pin_creation_button)).performClick()
			waitForTextEitherTree(R.string.pin_creation_error_weak_pin)

			setPinFields("313373", "313373")
			onNodeWithText(str(R.string.pin_creation_button)).performClick()

			waitForTextEitherTree(str(R.string.pin_creating_vault), assertDisplayed = false)

			waitForButton(str(R.string.camera_shutter_button_desc))

			onNode(
				hasRole(Role.Button) and hasContentDescription(str(R.string.camera_shutter_button_desc))
			).performClick()

			waitForContentDescriptionSimple(str(R.string.camera_more_options_content_description))

			onNode(
				hasRole(Role.Button) and hasContentDescription(str(R.string.camera_more_options_content_description))
			).performClick()

			waitForTestTagSimple("flash-switch")
			onNode(
				hasRole(Role.Switch) and hasTestTag("flash-switch")
			).performClick()

			onNode(
				hasRole(Role.Button) and hasContentDescription(str(R.string.camera_close_controls_content_description))
			).performClick()
		}
	}

	private fun waitForButton(contentDescription: String) {
		composeTestRule.waitUntil(
			timeoutMillis = 5.seconds.inWholeMilliseconds
		) {
			composeTestRule
				.onAllNodes(hasRole(Role.Button) and hasContentDescription(contentDescription))
				.fetchSemanticsNodes().isNotEmpty()
		}
	}

	private fun waitForTextSimple(@StringRes id: Int) {
		waitForTextSimple(str(id))
	}

	private fun waitForTextSimple(text: String) {
		composeTestRule.waitUntil(
			timeoutMillis = 5.seconds.inWholeMilliseconds
		) {
			composeTestRule
				.onAllNodes(hasText(text))
				.fetchSemanticsNodes().isNotEmpty()
		}
	}

	private fun waitForContentDescriptionSimple(contentDescription: String) {
		composeTestRule.waitUntil(
			timeoutMillis = 5.seconds.inWholeMilliseconds
		) {
			composeTestRule
				.onAllNodes(hasContentDescription(contentDescription))
				.fetchSemanticsNodes().isNotEmpty()
		}
	}

	private fun waitForTestTagSimple(testTag: String) {
		composeTestRule.waitUntil(
			timeoutMillis = 5.seconds.inWholeMilliseconds
		) {
			composeTestRule
				.onAllNodes(hasTestTag(testTag))
				.fetchSemanticsNodes().isNotEmpty()
		}
	}

	private fun ComposeContentTestRule.setPinFields(primary: String, confirm: String) {
		setTextField(
			placeholder = R.string.pin_creation_hint,
			value = primary,
		)

		setTextField(
			placeholder = R.string.pin_creation_confirm_hint,
			value = confirm,
		)
	}

	fun hasRole(role: Role): SemanticsMatcher =
		SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

	private fun str(@StringRes id: Int): String = r.getString(id)
	private val r: Resources
		get() {
			val application = ApplicationProvider.getApplicationContext<Application>()
			return application.resources
		}

	private fun ComposeContentTestRule.waitForText(
		@StringRes text: Int,
		timeout: Duration = 10.seconds
	) {
		this@waitForText.waitForText(str(text), timeout)
	}

	fun ComposeContentTestRule.waitForText(
		text: String,
		timeout: Duration = 10.seconds,
		useUnmergedTree: Boolean = true,
		substring: Boolean = true,
		assertDisplayed: Boolean = true
	) {
		waitUntil(timeout.inWholeMilliseconds) {
			onAllNodes(
				hasText(text, substring = substring),
				useUnmergedTree = useUnmergedTree
			).fetchSemanticsNodes().isNotEmpty()
		}
		val node = onNode(
			hasText(text, substring = substring),
			useUnmergedTree = useUnmergedTree
		)
		if (assertDisplayed) node.assertIsDisplayed() else node.assertExists()
	}

	fun ComposeContentTestRule.waitForTextEitherTree(
		text: String,
		timeout: Duration = 10.seconds,
		substring: Boolean = true,
		ignoreCase: Boolean = false,
		assertDisplayed: Boolean = true,
		preferUnmergedFirst: Boolean = true
	) {
		fun hasAny(useUnmerged: Boolean) = onAllNodes(
			hasText(text, substring = substring, ignoreCase = ignoreCase),
			useUnmergedTree = useUnmerged
		).fetchSemanticsNodes().isNotEmpty()

		val order = if (preferUnmergedFirst) listOf(true, false) else listOf(false, true)

		waitUntil(timeout.inWholeMilliseconds) {
			hasAny(order[0]) || hasAny(order[1])
		}

		// Pick the tree that actually has the node
		val chosenUnmerged = if (hasAny(order[0])) order[0] else order[1]

		val node = onNode(
			hasText(text, substring = substring, ignoreCase = ignoreCase),
			useUnmergedTree = chosenUnmerged
		)
		if (assertDisplayed) node.assertIsDisplayed() else node.assertExists()
	}

	fun ComposeContentTestRule.waitForTextEitherTree(
		@StringRes resId: Int,
		timeout: Duration = 10.seconds,
		substring: Boolean = true,
		ignoreCase: Boolean = false,
		assertDisplayed: Boolean = true,
		preferUnmergedFirst: Boolean = true
	) {
		val str = androidx.test.platform.app.InstrumentationRegistry
			.getInstrumentation().targetContext.getString(resId)
		waitForTextEitherTree(
			text = str,
			timeout = timeout,
			substring = substring,
			ignoreCase = ignoreCase,
			assertDisplayed = assertDisplayed,
			preferUnmergedFirst = preferUnmergedFirst
		)
	}

	fun ComposeContentTestRule.waitForContentDescription(
		text: String,
		timeout: Duration = 10.seconds,
		useUnmergedTree: Boolean = true,
		substring: Boolean = true
	) {
		waitUntil(timeout.inWholeMilliseconds) {
			onAllNodes(
				hasText(text, substring = substring),
				useUnmergedTree = useUnmergedTree
			).fetchSemanticsNodes().isNotEmpty()
		}
		onNodeWithContentDescription(text, substring = substring)
			.assertIsDisplayed()
	}

	private fun ComposeContentTestRule.setTextField(value: String, placeholder: Int) {
		onNode(
			hasSetTextAction() and hasTextExactly(
				str(placeholder),
				includeEditableText = false
			)
		).apply {
			performTextClearance()
			performTextInput(value)
		}
	}
}