import type { PaymentResponse } from '@/types/payment'

import type { VolumeChartPoint } from '@/components/VolumeChart'

const MS_PER_DAY = 24 * 60 * 60 * 1000

export function buildDailyVolume(
  payments: PaymentResponse[],
  days = 30,
): VolumeChartPoint[] {
  const now = Date.now()
  const start = now - days * MS_PER_DAY
  const buckets = new Map<string, number>()

  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(now - i * MS_PER_DAY)
    const key = d.toISOString().slice(0, 10)
    buckets.set(key, 0)
  }

  for (const p of payments) {
    const t = new Date(p.createdAt).getTime()
    if (Number.isNaN(t) || t < start) continue
    const key = p.createdAt.slice(0, 10)
    if (!buckets.has(key)) continue
    if (p.status === 'CANCELLED') continue
    buckets.set(key, (buckets.get(key) ?? 0) + p.amount)
  }

  return Array.from(buckets.entries()).map(([day, volumeMinor]) => ({ day, volumeMinor }))
}

export function sumVolumeMinorSuccessful(payments: PaymentResponse[]): number {
  return payments
    .filter((p) =>
      ['CAPTURED', 'REFUNDED', 'PARTIAL_REFUND'].includes(p.status),
    )
    .reduce((acc, p) => acc + p.amount, 0)
}

export function countRefundedStates(payments: PaymentResponse[]): number {
  return payments.filter((p) =>
    ['REFUNDED', 'PARTIAL_REFUND'].includes(p.status),
  ).length
}

export function countCapturedLike(payments: PaymentResponse[]): number {
  return payments.filter((p) =>
    ['CAPTURED', 'REFUNDED', 'PARTIAL_REFUND'].includes(p.status),
  ).length
}
