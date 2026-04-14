import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { VolumeChart } from '@/components/VolumeChart'
import { useOverviewPayments } from '@/hooks/useOverviewPayments'
import {
  buildDailyVolume,
  countCapturedLike,
  countRefundedStates,
  sumVolumeMinorSuccessful,
} from '@/lib/payment-analytics'
import { formatMinorUnits } from '@/lib/format'
import { selectIsAuthenticated, useAuthStore } from '@/stores/auth-store'

export function OverviewPage() {
  const isAuthed = useAuthStore(selectIsAuthenticated)
  const { data, isLoading, isError } = useOverviewPayments(isAuthed)

  if (!isAuthed) {
    return null
  }

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-28" />
          ))}
        </div>
        <Skeleton className="h-[300px]" />
      </div>
    )
  }

  if (isError || !data) {
    return (
      <p className="text-destructive text-sm">
        Could not load payments. Check the API key and that payment-service is running on port 8081.
      </p>
    )
  }

  const { payments, totalElements } = data
  const currency = payments[0]?.currency ?? 'USD'
  const volumeMinor = sumVolumeMinorSuccessful(payments)
  const successLike = countCapturedLike(payments)
  const refundedLike = countRefundedStates(payments)
  const successRate = payments.length > 0 ? successLike / payments.length : 0
  const refundRate = payments.length > 0 ? refundedLike / payments.length : 0
  const chartData = buildDailyVolume(payments, 30)

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-2xl font-semibold tracking-tight">Overview</h2>
        <p className="text-muted-foreground text-sm">
          KPIs use loaded payments (up to 2k rows). Total count uses the API total.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Total volume (loaded)</CardDescription>
            <CardTitle className="text-2xl">{formatMinorUnits(volumeMinor, currency)}</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-muted-foreground text-xs">CAPTURED + refunded states in sample</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Transactions</CardDescription>
            <CardTitle className="text-2xl">{totalElements}</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-muted-foreground text-xs">Total from payment-service</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Success rate (sample)</CardDescription>
            <CardTitle className="text-2xl">
              {payments.length ? `${Math.round(successRate * 100)}%` : '—'}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-muted-foreground text-xs">Captured or refunded in loaded rows</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Refund rate (sample)</CardDescription>
            <CardTitle className="text-2xl">
              {payments.length ? `${Math.round(refundRate * 100)}%` : '—'}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-muted-foreground text-xs">Refunded or partial in loaded rows</p>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Volume (30 days)</CardTitle>
          <CardDescription>Daily gross amount from loaded payments (excludes cancelled).</CardDescription>
        </CardHeader>
        <CardContent>
          <VolumeChart data={chartData} currency={currency} />
        </CardContent>
      </Card>
    </div>
  )
}
