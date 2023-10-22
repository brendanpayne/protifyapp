package com.protify.protifyapp.tests.features.login

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.protify.protifyapp.features.login.RegisterActivity
import org.junit.Rule
import org.junit.Test

class LoginTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testRegistration() {
        //Render Register Page
        composeTestRule.setContent { RegisterActivity().LandingPage {} }
        //Enter Email into field
        //composeTestRule.onNodeWithText("Email").performTextInput("test@gmail.com")
        //Enter Password into field
        //composeTestRule.onNodeWithText("Password").performTextInput("testpassword")
        //Click Create Account Button

        composeTestRule.onNodeWithText("Create Account").performClick()


    }
}