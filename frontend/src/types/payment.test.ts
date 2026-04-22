import { describe, expect, it } from 'vitest'

import { paymentListResponseSchema } from '@/types/payment'

describe('paymentListResponseSchema', () => {
  it('accepts clientSecret null from payment-service list responses', () => {
    const parsed = paymentListResponseSchema.parse({
      content: [
        {
          id: 'pay_1',
          amount: 1000,
          currency: 'USD',
          status: 'PENDING',
          description: null,
          clientSecret: null,
          metadata: {},
          card: null,
          createdAt: '2026-04-22T18:51:47.602480Z',
          expiresAt: '2026-04-22T19:51:47.602480Z',
          capturedAt: null,
          cancelledAt: null,
          totalRefunded: 0,
          amountRefunded: 0,
        },
      ],
      totalElements: 1,
      page: 0,
      size: 20,
    })
    expect(parsed.content[0].clientSecret).toBeNull()
  })
})
