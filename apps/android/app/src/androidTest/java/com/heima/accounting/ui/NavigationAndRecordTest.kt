package com.heima.accounting.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import com.heima.accounting.MainActivity
import java.time.LocalDate
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

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
        composeRule.onNodeWithContentDescription("收支类型：收入").performClick()
        composeRule.onNodeWithContentDescription("选择工资分类").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("收支类型：支出").performClick()
        composeRule.onNodeWithContentDescription("选择餐饮分类").assertIsDisplayed()
    }

    @Test
    fun secondaryCategoriesOnlyOpenAfterExplicitPrimarySelection() {
        composeRule.onNodeWithContentDescription("打开记账面板").performClick()
        assertTrue(composeRule.onAllNodesWithContentDescription("二级分类区域").fetchSemanticsNodes().isEmpty())

        composeRule.onNodeWithContentDescription("选择餐饮分类").performClick()
        composeRule.onNodeWithContentDescription("二级分类区域").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("选择午餐细分").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("收支类型：收入").performClick()
        assertTrue(composeRule.onAllNodesWithContentDescription("二级分类区域").fetchSemanticsNodes().isEmpty())

        composeRule.onNodeWithContentDescription("选择工资分类").performClick()
        composeRule.onNodeWithContentDescription("二级分类区域").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("选择奖金细分").assertIsDisplayed()
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
    fun savingWithoutPrimaryCategoryShowsARealValidationError() {
        composeRule.onNodeWithContentDescription("打开记账面板").performClick()
        enterOneYuan()
        composeRule.onNodeWithText("保存", substring = false).performClick()
        composeRule.onNodeWithText("请选择一个一级分类", substring = false).assertIsDisplayed()
    }

    @Test
    fun expenseCanBeSavedWithAnOptionalSecondaryCategory() {
        composeRule.onNodeWithContentDescription("打开记账面板").performClick()
        enterOneYuan()
        composeRule.onNodeWithContentDescription("选择餐饮分类").performClick()
        composeRule.onNodeWithContentDescription("选择午餐细分").performClick()
        composeRule.onNodeWithText("保存", substring = false).performClick()
        composeRule.onNodeWithText("账单已保存", substring = false).assertIsDisplayed()
    }

    @Test
    fun incomeCanBeSavedInQuickAndDetailedModes() {
        composeRule.onNodeWithContentDescription("打开记账面板").performClick()
        composeRule.onNodeWithContentDescription("收支类型：收入").performClick()
        enterOneYuan()
        composeRule.onNodeWithContentDescription("选择工资分类").performClick()
        composeRule.onNodeWithText("保存", substring = false).performClick()
        composeRule.onNodeWithText("账单已保存", substring = false).assertIsDisplayed()

        composeRule.onNodeWithContentDescription("打开记账面板").performClick()
        composeRule.onNodeWithContentDescription("收支类型：收入").performClick()
        enterOneYuan()
        composeRule.onNodeWithContentDescription("选择工资分类").performClick()
        composeRule.onNodeWithContentDescription("选择奖金细分").performClick()
        composeRule.onNodeWithText("保存", substring = false).performClick()
        composeRule.onNodeWithText("账单已保存", substring = false).assertIsDisplayed()
    }

    @Test
    fun statisticsPeriodSelectionHasOnePersistentSelectedState() {
        composeRule.onNodeWithContentDescription("打开统计").performClick()
        composeRule.onNodeWithContentDescription("统计时间范围：本周").performClick()
        composeRule.onNodeWithContentDescription("统计时间范围：本周").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("统计时间范围：本月").performClick()
        composeRule.onNodeWithContentDescription("统计时间范围：本月").assertIsDisplayed()
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
    fun everyExperienceSwitchHasDirectAndPersistentSemantics() {
        composeRule.onNodeWithContentDescription("打开我的").performClick()
        listOf("Liquid Glass 开关", "操作音效 开关", "触觉反馈 开关").forEach { description ->
            composeRule.onNodeWithContentDescription(description).assertIsOn().performClick().assertIsOff()
        }
        composeRule.onNodeWithContentDescription("减少动态效果 开关").assertIsOff().performClick().assertIsOn()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Liquid Glass 开关").assertIsOff()
        composeRule.onNodeWithContentDescription("操作音效 开关").assertIsOff()
        composeRule.onNodeWithContentDescription("触觉反馈 开关").assertIsOff()
        composeRule.onNodeWithContentDescription("减少动态效果 开关").assertIsOn()

        composeRule.onNodeWithContentDescription("Liquid Glass 开关").performClick().assertIsOn()
        composeRule.onNodeWithContentDescription("操作音效 开关").performClick().assertIsOn()
        composeRule.onNodeWithContentDescription("触觉反馈 开关").performClick().assertIsOn()
        composeRule.onNodeWithContentDescription("减少动态效果 开关").performClick().assertIsOff()
    }

    @Test
    fun customChineseDatePickerCanCancelAndConfirm() {
        val today = LocalDate.now()
        val nextMonth = today.plusMonths(1).withDayOfMonth(1)
        composeRule.onNodeWithContentDescription("打开记账面板").performClick()
        composeRule.onNodeWithContentDescription("选择记账日期").performClick()
        composeRule.onNodeWithText("选择日期").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("下一个月").performClick()
        composeRule.onNodeWithText("${nextMonth.year}年${nextMonth.monthValue}月", substring = false).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("${nextMonth.monthValue}月1日", substring = true).performClick()
        composeRule.onNodeWithText("取消", substring = false).performClick()
        composeRule.onNodeWithContentDescription("选择记账日期").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("选择记账日期").performClick()
        composeRule.onNodeWithContentDescription("下一个月").performClick()
        composeRule.onNodeWithContentDescription("${nextMonth.monthValue}月1日", substring = true).performClick()
        composeRule.onNodeWithText("确定", substring = false).performClick()
        composeRule.onNodeWithText("${nextMonth.monthValue}月1日", substring = true).assertIsDisplayed()
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

    private fun enterOneYuan() {
        composeRule.onNodeWithContentDescription("输入数字1").performClick()
    }
}
