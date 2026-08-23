package com.heima.accounting.domain

object DefaultCategories {
    private data class Definition(
        val id: String,
        val name: String,
        val icon: String,
        val color: Long,
        val children: List<String>,
    )

    private val expenseDefinitions = listOf(
        Definition("expense_food", "餐饮", "meal", 0xFFF2A65AL, listOf("早餐", "午餐", "晚餐", "外卖", "咖啡饮品", "零食水果", "聚餐", "其他")),
        Definition("expense_transport", "交通", "transport", 0xFF55A6D9L, listOf("公交", "地铁", "打车", "网约车", "加油", "停车", "高速", "火车", "机票", "车辆维护", "其他")),
        Definition("expense_shopping", "购物", "shopping", 0xFFE97868L, listOf("日用品", "服饰", "数码", "家居", "美妆", "礼物", "网购", "其他")),
        Definition("expense_housing", "居住", "home", 0xFF8A77D5L, listOf("房租", "房贷", "水费", "电费", "燃气", "物业", "宽带", "维修", "家具家电", "其他")),
        Definition("expense_entertainment", "娱乐", "entertainment", 0xFFDF729FL, listOf("游戏", "电影", "音乐", "旅行", "运动", "社交", "会员订阅", "其他")),
        Definition("expense_education", "教育", "education", 0xFF6B91D8L, listOf("书籍", "课程", "考试", "培训", "文具", "其他")),
        Definition("expense_health", "医疗健康", "health", 0xFFE35D6AL, listOf("药品", "门诊", "住院", "体检", "健身", "其他")),
        Definition("expense_social", "人情", "gift", 0xFFF0B349L, listOf("红包", "礼金", "捐赠", "其他")),
        Definition("expense_pet", "宠物", "pet", 0xFFCA8D63L, listOf("食品", "用品", "医疗", "美容", "其他")),
        Definition("expense_communication", "通讯网络", "router", 0xFF4F9BB8L, listOf("手机话费", "宽带网络", "邮寄快递", "数字服务", "其他")),
        Definition("expense_travel", "旅行", "travel", 0xFFF0A43CL, listOf("长途交通", "住宿", "景点门票", "当地出行", "旅行餐饮", "其他")),
        Definition("expense_parenting", "家庭育儿", "baby", 0xFFF09A8AL, listOf("奶粉辅食", "母婴用品", "托育", "教育", "医疗", "其他")),
        Definition("expense_utilities", "生活缴费", "receipt", 0xFF6AA0C8L, listOf("水费", "电费", "燃气", "物业", "税费", "其他")),
        Definition("expense_beauty", "服饰美容", "clothing", 0xFFCE739BL, listOf("服饰", "鞋包", "美妆", "理发", "护理", "其他")),
        Definition("expense_subscription", "订阅服务", "subscription", 0xFF8178CFL, listOf("影音会员", "软件服务", "游戏会员", "云存储", "其他")),
        Definition("expense_other", "其他", "other", 0xFF8A96A8L, listOf("意外支出", "税费", "育儿", "其他")),
    )

    private val incomeDefinitions = listOf(
        Definition("income_salary", "工资", "salary", 0xFF39A878L, listOf("基本工资", "奖金", "津贴", "加班")),
        Definition("income_part_time", "兼职", "part_time", 0xFF4AA6A6L, listOf("兼职", "项目", "自由职业")),
        Definition("income_investment", "投资收益", "investment", 0xFF5488D8L, listOf("利息", "股息", "理财收益", "其他")),
        Definition("income_business", "经营收入", "business", 0xFF8D73CBL, listOf("销售", "服务", "其他")),
        Definition("income_refund", "退款报销", "refund", 0xFF46A878L, listOf("退款", "报销")),
        Definition("income_gift", "红包礼金", "gift", 0xFFE46B74L, listOf("红包", "礼金", "其他")),
        Definition("income_other", "其他收入", "other", 0xFF8794A8L, listOf("闲置出售", "中奖", "其他")),
    )

    val all: List<Category> = buildList {
        addDefinitions(EntryType.EXPENSE, expenseDefinitions)
        addDefinitions(EntryType.INCOME, incomeDefinitions)
    }

    private fun MutableList<Category>.addDefinitions(
        type: EntryType,
        definitions: List<Definition>,
    ) {
        definitions.forEachIndexed { index, definition ->
            add(Category(definition.id, type, definition.name, definition.icon, definition.color, sortOrder = index))
            definition.children.forEachIndexed { childIndex, childName ->
                add(
                    Category(
                        id = "${definition.id}_${childName.stableId()}",
                        type = type,
                        name = childName,
                        iconKey = definition.icon,
                        colorArgb = definition.color,
                        parentId = definition.id,
                        sortOrder = childIndex,
                    ),
                )
            }
        }
    }

    private fun String.stableId(): String = buildString {
        for (character in this@stableId) append(character.code.toString(16))
    }
}
