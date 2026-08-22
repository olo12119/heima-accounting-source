package com.heima.accounting.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.heima.accounting.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NavigationAndRecordTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetVisualPreferences() {
        composeRule.activity
            .getSharedPreferences("heima_visual_preferences", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
    }

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
        composeRule.onNodeWithContentDescription("选择工资分类").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("切换到支出").performClick()
        composeRule.onNodeWithContentDescription("选择餐饮分类").assertIsDisplayed()
    }

    @Test
    fun realExpenseCanBeSavedWithPrimaryCategoryOnly() {
        composeRule.onNodeWithContentDescription("打开记账面板").performClick()
        composeRule.onNodeWithContentDescription("输入数字1").performClick()
        composeRule.onNodeWithContentDescription("输入数字2").performClick()
        composeRule.onNodeWithContentDescription("输入小数点").performClick()
        composeRule.onNodeWithContentDescription("输入数字5").performClick()
        composeRule.onNodeWithText("餐饮", substring = false).performClick()
        composeRule.onNodeWithText("保存", substring = false).performClick()
        composeRule.onNodeWithText("账单已保存", substring = false).assertIsDisplayed()
    }

    @Test
    fun experienceSwitchesAreIndependentAndThemeCanChange() {
        composeRule.onNodeWithContentDescription("打开我的").performClick()
        composeRule.onNodeWithContentDescription("自然治愈 主题").performClick()
        composeRule.onNodeWithContentDescription("自然治愈 主题，当前使用").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Liquid Glass 开关").performClick()
        composeRule.onNodeWithContentDescription("Liquid Glass 开关").assertIsOff()
        composeRule.onNodeWithContentDescription("操作音效 开关").assertIsOn()
        composeRule.onNodeWithContentDescription("触觉反馈 开关").assertIsOn()
    }

    @Test
    fun privacyToggleSurvivesNavigationWithinTheApp() {
        composeRule.onNodeWithContentDescription("隐藏所有金额").performClick()
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("显示所有金额").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("打开统计").performClick()
        composeRule.onAllNodesWithText("¥••••", substring = true)[0].assertIsDisplayed()
        composeRule.onNodeWithContentDescription("打开首页").performClick()
        composeRule.onNodeWithContentDescription("显示所有金额").assertIsDisplayed()
    }
}
