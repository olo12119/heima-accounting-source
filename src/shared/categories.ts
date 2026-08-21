import type { Category, EntryType } from './types'

export const CATEGORY_COLORS: Record<string, string> = {
  food: '#d98257',
  transport: '#4e7f8f',
  shopping: '#b16b86',
  housing: '#8a7657',
  entertainment: '#7a6bad',
  health: '#5f9277',
  learning: '#5579a7',
  communication: '#6d8795',
  social: '#b77a5e',
  travel: '#3f8c88',
  other: '#7c8580',
  salary: '#2d9b72',
  bonus: '#b58a3f',
  business: '#3f88a8',
  investment: '#7667b8',
  reimbursement: '#4d8e86',
  'income-gift': '#c16d87',
  'income-other': '#7b8796'
}

type CategoryDefinition = {
  id: string
  name: string
  icon: string
  entryType: EntryType
  children: Array<{ id: string; name: string }>
}

export const CATEGORY_DEFINITIONS: CategoryDefinition[] = [
  {
    id: 'food', name: '餐饮', icon: 'utensils', entryType: 'expense', children: [
      { id: 'food.breakfast', name: '早餐' }, { id: 'food.meal', name: '正餐' },
      { id: 'food.delivery', name: '外卖' }, { id: 'food.grocery', name: '买菜食材' },
      { id: 'food.snack', name: '零食' }, { id: 'food.drink', name: '咖啡茶饮' }
    ]
  },
  {
    id: 'transport', name: '交通', icon: 'car', entryType: 'expense', children: [
      { id: 'transport.public', name: '公交地铁' }, { id: 'transport.taxi', name: '打车' },
      { id: 'transport.bike', name: '共享骑行' }, { id: 'transport.fuel', name: '加油充电' },
      { id: 'transport.parking', name: '停车过路' }, { id: 'transport.maintenance', name: '车辆养护' }
    ]
  },
  {
    id: 'shopping', name: '购物', icon: 'shopping-bag', entryType: 'expense', children: [
      { id: 'shopping.daily', name: '日用百货' }, { id: 'shopping.clothing', name: '服饰鞋包' },
      { id: 'shopping.digital', name: '数码家电' }, { id: 'shopping.beauty', name: '美妆护理' },
      { id: 'shopping.home', name: '家居用品' }, { id: 'shopping.other', name: '其他购物' }
    ]
  },
  {
    id: 'housing', name: '居住', icon: 'house', entryType: 'expense', children: [
      { id: 'housing.rent', name: '房租房贷' }, { id: 'housing.utilities', name: '水电燃气' },
      { id: 'housing.property', name: '物业' }, { id: 'housing.repair', name: '装修维修' },
      { id: 'housing.housekeeping', name: '家政' }
    ]
  },
  {
    id: 'entertainment', name: '娱乐', icon: 'clapperboard', entryType: 'expense', children: [
      { id: 'entertainment.media', name: '影视会员' }, { id: 'entertainment.game', name: '游戏' },
      { id: 'entertainment.fitness', name: '运动健身' }, { id: 'entertainment.party', name: '聚会活动' },
      { id: 'entertainment.hobby', name: '兴趣爱好' }
    ]
  },
  {
    id: 'health', name: '医疗健康', icon: 'heart-pulse', entryType: 'expense', children: [
      { id: 'health.clinic', name: '挂号诊疗' }, { id: 'health.medicine', name: '药品' },
      { id: 'health.checkup', name: '体检' }, { id: 'health.insurance', name: '商业保险' },
      { id: 'health.care', name: '健康护理' }
    ]
  },
  {
    id: 'learning', name: '学习成长', icon: 'book-open', entryType: 'expense', children: [
      { id: 'learning.books', name: '书籍' }, { id: 'learning.course', name: '课程培训' },
      { id: 'learning.stationery', name: '文具' }, { id: 'learning.exam', name: '考试' },
      { id: 'learning.software', name: '软件工具' }
    ]
  },
  {
    id: 'communication', name: '通讯服务', icon: 'wifi', entryType: 'expense', children: [
      { id: 'communication.mobile', name: '手机话费' }, { id: 'communication.internet', name: '宽带网络' },
      { id: 'communication.delivery', name: '邮寄快递' }, { id: 'communication.digital', name: '数字服务' }
    ]
  },
  {
    id: 'social', name: '人情社交', icon: 'gift', entryType: 'expense', children: [
      { id: 'social.redpacket', name: '红包礼金' }, { id: 'social.treat', name: '请客' },
      { id: 'social.gift', name: '送礼' }, { id: 'social.charity', name: '公益捐赠' }
    ]
  },
  {
    id: 'travel', name: '旅行', icon: 'luggage', entryType: 'expense', children: [
      { id: 'travel.longdistance', name: '长途交通' }, { id: 'travel.hotel', name: '住宿' },
      { id: 'travel.ticket', name: '景点门票' }, { id: 'travel.local', name: '当地出行' },
      { id: 'travel.food', name: '旅行餐饮' }, { id: 'travel.other', name: '其他旅行' }
    ]
  },
  {
    id: 'other', name: '其他', icon: 'shapes', entryType: 'expense', children: [
      { id: 'other.pet', name: '宠物' }, { id: 'other.child', name: '育儿' },
      { id: 'other.tax', name: '税费' }, { id: 'other.unexpected', name: '意外支出' },
      { id: 'other.misc', name: '其他支出' }
    ]
  },
  {
    id: 'salary', name: '工资薪酬', icon: 'briefcase-business', entryType: 'income', children: [
      { id: 'salary.base', name: '工资' }, { id: 'salary.allowance', name: '津贴补助' },
      { id: 'salary.parttime', name: '兼职收入' }, { id: 'salary.pension', name: '退休金' }
    ]
  },
  {
    id: 'bonus', name: '奖金福利', icon: 'badge-dollar-sign', entryType: 'income', children: [
      { id: 'bonus.performance', name: '绩效奖金' }, { id: 'bonus.yearend', name: '年终奖' },
      { id: 'bonus.benefit', name: '福利补贴' }, { id: 'bonus.other', name: '其他奖金' }
    ]
  },
  {
    id: 'business', name: '经营所得', icon: 'store', entryType: 'income', children: [
      { id: 'business.sales', name: '销售收入' }, { id: 'business.service', name: '服务收入' },
      { id: 'business.royalty', name: '稿费版权' }, { id: 'business.other', name: '其他经营' }
    ]
  },
  {
    id: 'investment', name: '理财收益', icon: 'chart-no-axes-combined', entryType: 'income', children: [
      { id: 'investment.interest', name: '利息' }, { id: 'investment.dividend', name: '分红' },
      { id: 'investment.fund', name: '基金股票' }, { id: 'investment.other', name: '其他理财' }
    ]
  },
  {
    id: 'reimbursement', name: '报销退款', icon: 'receipt-text', entryType: 'income', children: [
      { id: 'reimbursement.work', name: '工作报销' }, { id: 'reimbursement.refund', name: '购物退款' },
      { id: 'reimbursement.medical', name: '医疗报销' }, { id: 'reimbursement.other', name: '其他报销' }
    ]
  },
  {
    id: 'income-gift', name: '人情红包', icon: 'hand-coins', entryType: 'income', children: [
      { id: 'income-gift.redpacket', name: '红包' }, { id: 'income-gift.gift', name: '礼金' },
      { id: 'income-gift.support', name: '亲友资助' }
    ]
  },
  {
    id: 'income-other', name: '其他收入', icon: 'circle-dollar-sign', entryType: 'income', children: [
      { id: 'income-other.prize', name: '中奖' }, { id: 'income-other.sale', name: '闲置转卖' },
      { id: 'income-other.misc', name: '其他收入' }
    ]
  }
]

export const CATEGORIES: Category[] = CATEGORY_DEFINITIONS.flatMap((primary, primaryIndex) => [
  { id: primary.id, parentId: null, name: primary.name, icon: primary.icon, sortOrder: primaryIndex, entryType: primary.entryType },
  ...primary.children.map((secondary, secondaryIndex) => ({
    id: secondary.id,
    parentId: primary.id,
    name: secondary.name,
    icon: primary.icon,
    sortOrder: secondaryIndex,
    entryType: primary.entryType
  }))
])

export const getCategoryColor = (categoryId: string): string => CATEGORY_COLORS[categoryId] ?? '#7c8580'
