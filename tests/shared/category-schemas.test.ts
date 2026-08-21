import { describe, expect, it } from 'vitest'
import {
  customPrimaryCategoryInputSchema,
  customSecondaryCategoryInputSchema
} from '../../src/shared/schemas'

describe('自定义分类输入校验', () => {
  it('接受合法名称、图标和预设颜色', () => {
    expect(customPrimaryCategoryInputSchema.parse({
      entryType: 'expense', name: '家庭生活', firstSecondaryName: '家庭日用', icon: 'house', color: '#5579a7'
    }).name).toBe('家庭生活')
    expect(customSecondaryCategoryInputSchema.parse({ parentId: 'food', name: '公司食堂' }).name).toBe('公司食堂')
  })

  it('拒绝空名称、过长名称、未知图标和任意颜色', () => {
    expect(() => customPrimaryCategoryInputSchema.parse({
      entryType: 'expense', name: '', firstSecondaryName: '明细', icon: 'house', color: '#5579a7'
    })).toThrow()
    expect(() => customSecondaryCategoryInputSchema.parse({ parentId: 'food', name: '这是一个超过十六个汉字长度限制的分类名称' })).toThrow()
    expect(() => customPrimaryCategoryInputSchema.parse({
      entryType: 'expense', name: '测试', firstSecondaryName: '明细', icon: 'unknown', color: '#123456'
    })).toThrow()
  })
})
