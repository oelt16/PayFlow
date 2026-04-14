import { describe, expect, it } from 'vitest'

import { refundFormSchema } from '@/lib/refund-form-schema'

describe('refundFormSchema', () => {
  it('accepts valid payload', () => {
    const r = refundFormSchema.safeParse({
      paymentId: 'pay_1',
      amount: 500,
      currency: 'usd',
      reason: 'duplicate',
    })
    expect(r.success).toBe(true)
    if (r.success) expect(r.data.currency).toBe('USD')
  })

  it('rejects empty payment id', () => {
    const r = refundFormSchema.safeParse({
      paymentId: '',
      amount: 1,
      currency: 'USD',
    })
    expect(r.success).toBe(false)
  })

  it('rejects amount below 1', () => {
    const r = refundFormSchema.safeParse({
      paymentId: 'pay_1',
      amount: 0,
      currency: 'USD',
    })
    expect(r.success).toBe(false)
  })

  it('rejects wrong currency length', () => {
    const r = refundFormSchema.safeParse({
      paymentId: 'pay_1',
      amount: 100,
      currency: 'US',
    })
    expect(r.success).toBe(false)
  })
})
