package com.heima.accounting.ui

enum class AppDestination(
    val label: String,
    val accessibilityLabel: String,
) {
    HOME(label = "首页", accessibilityLabel = "打开首页"),
    STATISTICS(label = "统计", accessibilityLabel = "打开统计"),
    RECORD(label = "记账", accessibilityLabel = "打开记账面板"),
    BUDGET(label = "预算", accessibilityLabel = "打开预算"),
    PROFILE(label = "我的", accessibilityLabel = "打开我的"),
}

