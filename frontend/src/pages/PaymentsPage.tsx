import { useState } from 'react'

import { PaymentTable } from '@/components/PaymentTable'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { usePaymentsList } from '@/hooks/usePayments'
import { PAYMENT_STATUSES } from '@/types/payment'
import { selectIsAuthenticated, useAuthStore } from '@/stores/auth-store'

export function PaymentsPage() {
  const isAuthed = useAuthStore(selectIsAuthenticated)
  const [page, setPage] = useState(0)
  const [size] = useState(20)
  const [status, setStatus] = useState<string | undefined>(undefined)

  const { data, isLoading, isError } = usePaymentsList(page, size, status, isAuthed)

  if (!isAuthed) return null

  const totalPages = data ? Math.max(1, Math.ceil(data.totalElements / data.size)) : 1

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-2xl font-semibold tracking-tight">Payments</h2>
          <p className="text-muted-foreground text-sm">Paginated list from payment-service.</p>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-muted-foreground text-sm">Status</span>
          <Select
            value={status ?? 'ALL'}
            onValueChange={(v) => {
              setStatus(!v || v === 'ALL' ? undefined : v)
              setPage(0)
            }}
          >
            <SelectTrigger className="w-[200px]">
              <SelectValue placeholder="All statuses" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All</SelectItem>
              {PAYMENT_STATUSES.map((s) => (
                <SelectItem key={s} value={s}>
                  {s}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Transactions</CardTitle>
          <CardDescription>
            {data
              ? `Page ${data.page + 1} of ${totalPages} · ${data.totalElements} total`
              : 'Loading…'}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {isLoading ? (
            <Skeleton className="h-48 w-full" />
          ) : isError ? (
            <p className="text-destructive text-sm">Could not load payments.</p>
          ) : data ? (
            <>
              <PaymentTable payments={data.content} />
              <div className="flex items-center justify-between gap-2">
                <Button
                  type="button"
                  variant="outline"
                  disabled={page <= 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  Previous
                </Button>
                <span className="text-muted-foreground text-sm">
                  Page {data.page + 1} / {totalPages}
                </span>
                <Button
                  type="button"
                  variant="outline"
                  disabled={page + 1 >= totalPages}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Next
                </Button>
              </div>
            </>
          ) : null}
        </CardContent>
      </Card>
    </div>
  )
}
