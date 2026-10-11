package com.decisionpro.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.filters.LargeTest
import com.decisionpro.app.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@LargeTest
class HomeScreenTest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun allToolCardsAreDisplayed() {
        composeRule.onNodeWithText("Decision Matrix").assertIsDisplayed()
        composeRule.onNodeWithText("Decision Tree").assertIsDisplayed()
        composeRule.onNodeWithText("Marble Run").assertIsDisplayed()
    }

    @Test
    fun tappingTreeCardNavigatesToTreeScreen() {
        composeRule.onNodeWithText("Decision Tree").performClick()
        composeRule.onNodeWithText("Best expected value").assertExists()
    }
}
