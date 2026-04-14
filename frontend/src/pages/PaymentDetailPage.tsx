import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { PaymentStatusBadge } from '@/components/PaymentStatusBadge'
import { Button, buttonVariants } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Skeleton } from '@/components/ui/skeleton'
import {
  useCancelPayment,
  useCapturePayment,
  usePayment,
  useRefunds,
} from '@/hooks/usePayments'
import { formatIsoDate, formatMinorUnits } from '@/lib/format'
import { toastApiError } from '@/lib/toast-error'
import { cn } from '@/lib/utils'
import { selectIsAuthenticated, useAuthStore } from '@/stores/auth-store'

export function PaymentDetailPage() {
  const { id } = useParams<{ id: string }>()
  const isAuthed = useAuthStore(selectIsAuthenticated)
  const paymentId = id ?? ''

  const { data: payment, isLoading, isError } = usePayment(paymentId, isAuthed)
  const { data: refunds } = useRefunds(paymentId, isAuthed)

  const captureMut = useCapturePayment()
  const cancelMut = useCancelPayment()

  const [cancelOpen, setCancelOpen] = useState(false)
  const [cancelReason, setCancelReason] = useState('')

  if (!isAuthed) return null

  if (isLoading) {
    return <Skeleton className="h-64 w-full max-w-3xl" />
  }

  if (isError || !payment) {
    return (
      <div className="space-y-4">
        <p className="text-destructive text-sm">Payment not found or request failed.</p>
        <Link to="/payments" className={cn(buttonVariants({ variant: 'outline' }))}>
          Back to payments
        </Link>
      </div>
    )
  }

  const canCapture = payment.status === 'PENDING'
  const canCancel = payment.status === 'PENDING'
  const canRefund = payment.status === 'CAPTURED' || payment.status === 'PARTIAL_REFUND'

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <Link
            to="/payments"
            className={cn(buttonVariants({ variant: 'ghost', size: 'sm' }), 'mb-2 -ml-2 inline-flex')}
          >
            ← Payments
          </Link>
          <h2 className="font-mono text-lg tracking-tight">{payment.id}</h2>
          <div className="mt-2 flex items-center gap-2">
            <PaymentStatusBadge status={payment.status} />
            <span className="text-muted-foreground text-sm">
              {formatMinorUnits(payment.amount, payment.currency)}
            </span>
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          {canCapture ? (
            <Button
              type="button"
              disabled={captureMut.isPending}
              onClick={async () => {
                try {
                  await captureMut.mutateAsync(payment.id)
                } catch (e) {
                  toastApiError(e)
                }
              }}
            >
              Capture
            </Button>
          ) : null}
          {canCancel ? (
            <Button type="button" variant="secondary" onClick={() => setCancelOpen(true)}>
              Cancel
            </Button>
          ) : null}
          {canRefund ? (
            <Link
              to={`/refunds?paymentId=${encodeURIComponent(payment.id)}&currency=${encodeURIComponent(payment.currency)}`}
              className={cn(buttonVariants({ variant: 'outline' }))}
            >
              Refund
            </Link>
          ) : null}
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Timeline</CardTitle>
          <CardDescription>Key timestamps from the payment record.</CardDescription>
        </CardHeader>
        <CardContent>
          <ol className="relative space-y-4 border-s border-border ps-4">
            <li>
              <span className="text-muted-foreground text-xs">Created</span>
              <p className="text-sm">{formatIsoDate(payment.createdAt)}</p>
            </li>
            {payment.capturedAt ? (
              <li>
                <span className="text-muted-foreground text-xs">Captured</span>
                <p className="text-sm">{formatIsoDate(payment.capturedAt)}</p>
              </li>
            ) : null}
            {payment.cancelledAt ? (
              <li>
                <span className="text-muted-foreground text-xs">Cancelled</span>
                <p className="text-sm">{formatIsoDate(payment.cancelledAt)}</p>
              </li>
            ) : null}
            {payment.expiresAt ? (
              <li>
                <span className="text-muted-foreground text-xs">Expires</span>
                <p className="text-sm">{formatIsoDate(payment.expiresAt)}</p>
              </li>
            ) : null}
          </ol>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Card</CardTitle>
        </CardHeader>
        <CardContent>
          {payment.card ? (
            <dl className="grid gap-2 text-sm">
              <div className="flex justify-between">
                <dt className="text-muted-foreground">Brand</dt>
                <dd>{payment.card.brand}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-muted-foreground">Last 4</dt>
                <dd>{payment.card.last4}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-muted-foreground">Expiry</dt>
                <dd>
                  {payment.card.expMonth}/{payment.card.expYear}
                </dd>
              </div>
            </dl>
          ) : (
            <p className="text-muted-foreground text-sm">No card stub on this payment.</p>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Refunds</CardTitle>
          <CardDescription>
            Refunded: {formatMinorUnits(payment.totalRefunded ?? 0, payment.currency)}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {!refunds?.data.length ? (
            <p className="text-muted-foreground text-sm">No refunds yet.</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>ID</TableHead>
                  <TableHead>Amount</TableHead>
                  <TableHead>Created</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {refunds.data.map((r) => (
                  <TableRow key={r.id}>
                    <TableCell className="font-mono text-xs">{r.id}</TableCell>
                    <TableCell>{formatMinorUnits(r.amount, r.currency)}</TableCell>
                    <TableCell className="text-muted-foreground text-sm">
                      {formatIsoDate(r.createdAt)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Dialog open={cancelOpen} onOpenChange={setCancelOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Cancel payment</DialogTitle>
            <DialogDescription>
              Only PENDING payments can be cancelled. This cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            <Label htmlFor="cancelReason">Reason (optional)</Label>
            <Input
              id="cancelReason"
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
            />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setCancelOpen(false)}>
              Close
            </Button>
            <Button
              type="button"
              variant="destructive"
              disabled={cancelMut.isPending}
              onClick={async () => {
                try {
                  await cancelMut.mutateAsync({
                    id: payment.id,
                    reason: cancelReason.trim() || undefined,
                  })
                  setCancelOpen(false)
                  setCancelReason('')
                } catch (e) {
                  toastApiError(e)
                }
              }}
            >
              Confirm cancel
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
