import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'

import { PaymentTable } from '@/components/PaymentTable'
import type { PaymentResponse } from '@/types/payment'

const sample: PaymentResponse[] = [
  {
    id: 'pay_1',
    amount: 1000,
    currency: 'USD',
    status: 'PENDING',
    description: null,
    clientSecret: 'cs_1',
    metadata: {},
    card: { last4: '4242', brand: 'VISA', expMonth: 12, expYear: 2027 },
    createdAt: '2025-01-01T12:00:00Z',
    expiresAt: null,
    capturedAt: null,
    cancelledAt: null,
    totalRefunded: 0,
    amountRefunded: 0,
  },
]

describe('PaymentTable', () => {
  it('renders rows and links to detail', () => {
    render(
      <MemoryRouter>
        <PaymentTable payments={sample} />
      </MemoryRouter>,
    )
    expect(screen.getByText('pay_1')).toBeInTheDocument()
    const link = screen.getByRole('link', { name: 'pay_1' })
    expect(link).toHaveAttribute('href', '/payments/pay_1')
  })

  it('shows empty state', () => {
    render(
      <MemoryRouter>
        <PaymentTable payments={[]} />
      </MemoryRouter>,
    )
    expect(screen.getByText(/no payments yet/i)).toBeInTheDocument()
  })
})
