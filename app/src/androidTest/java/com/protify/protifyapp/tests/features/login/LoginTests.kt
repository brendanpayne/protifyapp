package com.protify.protifyapp.tests.features.login

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import com.protify.protifyapp.AccountActivity
import com.protify.protifyapp.ui.theme.ProtifyTheme
import org.junit.Rule
import org.junit.Test

class LoginTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testLogin() {
        // Given
        val testEmail = "test@email.com"
        val testPassword = "Password!"

        // When
        loginToTestAccount(testEmail, testPassword)

        // Then
        composeTestRule.onNode(hasText("Good Morning, ${testEmail}!")).assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    private fun loginToTestAccount(testEmail: String, testPassword: String) {
        composeTestRule.setContent {
            ProtifyTheme {
                AccountActivity().AccountPage()
            }
        }

        composeTestRule.onNodeWithText("Existing User").performClick()

        composeTestRule.onNodeWithText("Email").performClick()
        composeTestRule.onNodeWithText("Email").performTextInput(testEmail)
        composeTestRule.onNodeWithText("Password").performClick()
        composeTestRule.onNodeWithText("Password").performTextInput(testPassword)

        composeTestRule.onNodeWithText("Login").performClick()

        composeTestRule.waitUntilDoesNotExist(hasText("Login"))
    }
}

