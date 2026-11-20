package com.example

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.AppThemeMode
import com.example.data.catalog.CuratedCatalog
import com.example.ui.components.GoogleSignInSheet
import com.example.ui.screens.ExploreToolsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.theme.CreovaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CreovaAppRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `home renders in Arabic RTL by default and toggles to English LTR`() {
        var language by mutableStateOf(AppLanguage.AR)
        var capturedDirection: LayoutDirection? = null

        composeRule.setContent {
            CreovaTheme(themeMode = AppThemeMode.DARK, language = language) {
                capturedDirection = LocalLayoutDirection.current
                HomeScreen(
                    language = language,
                    tools = CuratedCatalog.verifiedTools,
                    resources = CuratedCatalog.curatedResources,
                    posts = CuratedCatalog.editorialPosts,
                    creators = CuratedCatalog.editorialCreators,
                    events = emptyList(),
                    savedKeys = emptySet(),
                    unreadNotificationsCount = 0,
                    onToggleLanguage = {
                        language = if (language == AppLanguage.AR) AppLanguage.EN else AppLanguage.AR
                    },
                    onOpenSearch = {},
                    onOpenNotifications = {},
                    onOpenAiAssistant = {},
                    onSelectToolCategory = {},
                    onOpenTool = {},
                    onOpenResource = {},
                    onOpenPost = {},
                    onOpenCreator = {},
                    onToggleSaveTool = {},
                    onSeeAllTools = {},
                    onSeeAllLibrary = {},
                    onSeeAllCommunity = {},
                    contentPadding = PaddingValues(0.dp)
                )
            }
        }

        assertEquals(LayoutDirection.Rtl, capturedDirection)
        composeRule.onNodeWithText("كل أدوات المبدعين في مكان واحد").assertIsDisplayed()

        composeRule.onNodeWithTag("home_language_toggle").performClick()
        composeRule.waitForIdle()

        assertEquals(LayoutDirection.Ltr, capturedDirection)
        composeRule.onNodeWithText("One Universe for Every Creator").assertIsDisplayed()
    }

    @Test
    fun `guest can browse tools directory and library cleanly`() {
        var openedToolId: String? = null
        composeRule.setContent {
            CreovaTheme(themeMode = AppThemeMode.LIGHT, language = AppLanguage.EN) {
                ExploreToolsScreen(
                    language = AppLanguage.EN,
                    tools = CuratedCatalog.verifiedTools,
                    savedKeys = emptySet(),
                    initialCategory = null,
                    onOpenTool = { openedToolId = it },
                    onToggleSaveTool = {},
                    contentPadding = PaddingValues(0.dp)
                )
            }
        }

        composeRule.onNodeWithText("Creative Tools Directory").assertIsDisplayed()
        composeRule.onNodeWithTag("tool_card_capcut").performClick()
        assertEquals("capcut", openedToolId)
    }

    @Test
    fun `library screen displays curated categories and triggers submit callback`() {
        var submitClicked = false
        composeRule.setContent {
            CreovaTheme(themeMode = AppThemeMode.DARK, language = AppLanguage.EN) {
                LibraryScreen(
                    language = AppLanguage.EN,
                    resources = CuratedCatalog.curatedResources,
                    savedKeys = emptySet(),
                    likedKeys = emptySet(),
                    onOpenResource = {},
                    onToggleSaveResource = {},
                    onToggleLikeResource = {},
                    onOpenSubmitResource = { submitClicked = true },
                    contentPadding = PaddingValues(0.dp)
                )
            }
        }

        composeRule.onNodeWithText("Creative Resource Library").assertIsDisplayed()
        composeRule.onNodeWithTag("submit_resource_button").performClick()
        assertTrue(submitClicked)
    }

    @Test
    fun `guest profile screen displays google sign in button and invokes callback`() {
        var signInClicked = false
        composeRule.setContent {
            CreovaTheme(themeMode = AppThemeMode.DARK, language = AppLanguage.AR) {
                com.example.ui.screens.ProfileScreen(
                    currentUser = null,
                    myProfile = null,
                    isSigningIn = false,
                    isAdmin = false,
                    savedCount = 0,
                    unreadNotificationsCount = 0,
                    onSignInClick = { signInClicked = true },
                    onOpenSavedItems = {},
                    onOpenNotifications = {},
                    onOpenSettings = {},
                    onOpenAiAssistant = {},
                    onOpenAdminModeration = {},
                    onUpdateProfile = { _, _, _, _, _ -> },
                    contentPadding = PaddingValues(0.dp)
                )
            }
        }

        composeRule.onNodeWithTag("google_signin_button").assertIsDisplayed()
        composeRule.onNodeWithTag("google_signin_button").performClick()
        assertTrue(signInClicked)
    }
}
