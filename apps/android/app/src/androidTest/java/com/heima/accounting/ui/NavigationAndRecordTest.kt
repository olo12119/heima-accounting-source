package com.heima.accounting.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.heima.accounting.MainActivity
import org.junit.Rule
import org.junit.Test

class NavigationAndRecordTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun fiveEntryNavigation_reachesEveryRealScreen() {
        composeRule.onNodeWithContentDescription("打开统计").performClick()
        composeRule.onNodeWithText("读懂每一笔真实收支").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("打开预算").performClick()
        composeRule.onNodeWithText("给生活留一点从容").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("打开我的").performClick()
        composeRule.onNodeWithText("只属于你的本地账本").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("打开首页").performClick()
        composeRule.onNodeWithText("今日消费").assertIsDisplayed()
    }

    @Test
    fun amountPad_displaysExactlyWhatTheUserTypes() {
        composeRule.onNodeWithContentDescription("打开记账面板").performClick()
        composeRule.onNodeWithContentDescription("输入数字1").performClick()
        composeRule.onNodeWithContentDescription("输入数字2").performClick()
        composeRule.onNodeWithContentDescription("输入小数点").performClick()
        composeRule.onNodeWithContentDescription("输入数字5").performClick()

        composeRule.onNodeWithText("12.5").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("输入数字0").performClick()
        composeRule.onNodeWithText("12.50").assertIsDisplayed()
    }

    @Test
    fun incomeExpenseSwitch_remainsOperable() {
        composeRule.onNodeWithContentDescription("打开记账面板").performClick()
        composeRule.onNodeWithContentDescription("切换到收入").performClick()
        composeRule.onNodeWithText("工资", substring = false).assertIsDisplayed()

        composeRule.onNodeWithContentDescription("切换到支出").performClick()
        composeRule.onNodeWithText("餐饮", substring = false).assertIsDisplayed()
    }
}
