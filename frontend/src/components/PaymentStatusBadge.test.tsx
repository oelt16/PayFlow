import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { PaymentStatusBadge } from '@/components/PaymentStatusBadge'

describe('PaymentStatusBadge', () => {
  it.each([
    ['PENDING', 'PENDING'],
    ['CAPTURED', 'CAPTURED'],
    ['CANCELLED', 'CANCELLED'],
    ['REFUNDED', 'REFUNDED'],
    ['PARTIAL_REFUND', 'PARTIAL_REFUND'],
    ['EXPIRED', 'EXPIRED'],
  ])('renders %s', (status, label) => {
    render(<PaymentStatusBadge status={status} />)
    expect(screen.getByText(label)).toBeInTheDocument()
  })

  it('renders unknown status as text', () => {
    render(<PaymentStatusBadge status="UNKNOWN" />)
    expect(screen.getByText('UNKNOWN')).toBeInTheDocument()
  })
})
