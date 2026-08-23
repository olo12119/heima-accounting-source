package com.heima.accounting.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipe
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
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
    fun horizontalPagerSwitchesOnlyPersistentPagesAndNeverOpensRecordSheet() {
        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("读懂每一笔真实收支").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("清空", substring = false).fetchSemanticsNodes().isEmpty())

        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("给生活留一点从容").assertIsDisplayed()

        composeRule.onRoot().performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("读懂每一笔真实收支").assertIsDisplayed()
    }

    @Test
    fun draggingBottomLensSnapsToTabsAndDoesNotOpenRecordWhilePassingIt() {
        composeRule.onNodeWithContentDescription("底部导航选中镜片，可左右拖动").performTouchInput {
            swipe(
                start = center,
                end = Offset(center.x + visibleSize.width * 1.15f, center.y),
                durationMillis = 500,
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("读懂每一笔真实收支").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("底部导航选中镜片，可左右拖动").performTouchInput {
            swipe(
                start = center,
                end = Offset(center.x + visibleSize.width * 2.15f, center.y),
                durationMillis = 650,
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("给生活留一点从容").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("清空", substring = false).fetchSemanticsNodes().isEmpty())
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
    fun experienceSwitchesChangeTheActualAppBehaviorDescription() {
        composeRule.onNodeWithContentDescription("打开我的").performClick()
        setSwitch("Liquid Glass 开关", enabled = true)
        composeRule.onNodeWithContentDescription("Liquid Glass 开关").performClick().assertIsOff()
        composeRule.onNodeWithContentDescription("Liquid Glass 已关闭", substring = true).assertIsDisplayed()

        setSwitch("操作音效 开关", enabled = true)
        composeRule.onNodeWithContentDescription("操作音效 开关").performClick().assertIsOff()
        composeRule.onNodeWithContentDescription("操作音效已关闭", substring = true).assertIsDisplayed()

        setSwitch("触觉反馈 开关", enabled = true)
        composeRule.onNodeWithContentDescription("触觉反馈 开关").performClick().assertIsOff()
        composeRule.onNodeWithContentDescription("触觉反馈已关闭", substring = true).assertIsDisplayed()

        setSwitch("减少动态效果 开关", enabled = false)
        composeRule.onNodeWithContentDescription("减少动态效果 开关").performClick().assertIsOn()
        composeRule.onNodeWithContentDescription("减少动态效果已开启", substring = true).assertIsDisplayed()
    }

    @Test
    fun liquidGlassOnAndOffProduceVisiblyDifferentRendering() {
        composeRule.onNodeWithContentDescription("打开我的").performClick()
        setSwitch("Liquid Glass 开关", enabled = true)
        composeRule.waitForIdle()
        val enabled = composeRule.onRoot().captureToImage().toPixelMap()

        composeRule.onNodeWithContentDescription("Liquid Glass 开关").performClick()
        composeRule.waitForIdle()
        val disabled = composeRule.onRoot().captureToImage().toPixelMap()
        var changedSamples = 0
        for (y in 0 until minOf(enabled.height, disabled.height) step 24) {
            for (x in 0 until minOf(enabled.width, disabled.width) step 24) {
                if (enabled[x, y] != disabled[x, y]) changedSamples++
            }
        }
        assertTrue("Liquid Glass ON/OFF 应产生可见材质差异", changedSamples > 20)
    }

    @Test
    fun natureThemeHasIndependentLightAndDarkRendering() {
        composeRule.onNodeWithContentDescription("打开我的").performClick()
        composeRule.onNodeWithContentDescription("自然治愈 主题").performClick()
        composeRule.onNodeWithContentDescription("设置选项：浅色").performClick()
        composeRule.waitForIdle()
        val light = composeRule.onRoot().captureToImage().toPixelMap()

        composeRule.onNodeWithContentDescription("设置选项：深色").performClick()
        composeRule.waitForIdle()
        val dark = composeRule.onRoot().captureToImage().toPixelMap()
        var changedSamples = 0
        for (y in 0 until minOf(light.height, dark.height) step 24) {
            for (x in 0 until minOf(light.width, dark.width) step 24) {
                if (light[x, y] != dark[x, y]) changedSamples++
            }
        }
        assertTrue("自然主题应拥有独立的浅色与深色渲染", changedSamples > 120)
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
    fun statisticsSupportsCustomSingleDayAndRangeWithoutSystemPicker() {
        composeRule.onNodeWithContentDescription("打开统计").performClick()
        composeRule.onNodeWithContentDescription("自定义统计日期").performClick()
        composeRule.onNodeWithText("自定义统计日期").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("自定义日期模式：单日").performClick()
        composeRule.onNodeWithText("确定", substring = false).performClick()
        composeRule.onNodeWithText("自定义：", substring = true).assertIsDisplayed()

        composeRule.onNodeWithText("修改", substring = false).performClick()
        composeRule.onNodeWithContentDescription("自定义日期模式：日期区间").performClick()
        val today = LocalDate.now()
        composeRule.onNodeWithContentDescription("${today.monthValue}月${today.dayOfMonth}日", substring = true).performClick()
        composeRule.onNodeWithText("请选择结束日期", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("取消", substring = false).performClick()
        composeRule.onNodeWithText("自定义：", substring = true).assertIsDisplayed()

        composeRule.onNodeWithText("重置", substring = false).performClick()
        composeRule.onNodeWithContentDescription("统计时间范围：本月").assertIsDisplayed()
    }

    @Test
    fun deletionSnackbarCanUndoAndAlsoExpiresAutomatically() {
        saveOneExpense()
        composeRule.onNodeWithContentDescription("打开我的").performClick()
        composeRule.onRoot().performTouchInput { swipeUp() }
        composeRule.onRoot().performTouchInput { swipeUp() }
        composeRule.onNodeWithText("全部账单", substring = false).performClick()
        composeRule.onAllNodesWithText("删除", substring = false)[0].performClick()
        composeRule.onNodeWithText("删除这笔账单？").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("对话框操作：删除").performClick()
        composeRule.onNodeWithText("账单已删除", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("撤销", substring = false).performClick()
        composeRule.onNodeWithText("已恢复这笔账单", substring = false).assertIsDisplayed()

        composeRule.onAllNodesWithText("删除", substring = false)[0].performClick()
        composeRule.onNodeWithContentDescription("对话框操作：删除").performClick()
        composeRule.onNodeWithText("账单已删除", substring = false).assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 6_500) {
            composeRule.onAllNodesWithText("账单已删除", substring = false).fetchSemanticsNodes().isEmpty()
        }
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

    private fun saveOneExpense() {
        composeRule.onNodeWithContentDescription("打开记账面板").performClick()
        enterOneYuan()
        composeRule.onNodeWithContentDescription("选择餐饮分类").performClick()
        composeRule.onNodeWithText("保存", substring = false).performClick()
        composeRule.onNodeWithText("账单已保存", substring = false).assertIsDisplayed()
    }

    private fun setSwitch(description: String, enabled: Boolean) {
        val node = composeRule.onNodeWithContentDescription(description)
        val current = node.fetchSemanticsNode().config[SemanticsProperties.ToggleableState] == ToggleableState.On
        if (current != enabled) node.performClick()
        if (enabled) node.assertIsOn() else node.assertIsOff()
    }
}
