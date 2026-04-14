import * as React from 'react'

import { zodResolver } from '@hookform/resolvers/zod'
import { useForm, type Resolver } from 'react-hook-form'

import { Button } from '@/components/ui/button'
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
import { refundFormSchema, type RefundFormValues } from '@/lib/refund-form-schema'

export type RefundFormProps = {
  defaultPaymentId?: string
  defaultCurrency?: string
  onSubmit: (values: RefundFormValues) => Promise<void>
  isSubmitting?: boolean
}

export function RefundForm({
  defaultPaymentId = '',
  defaultCurrency = 'USD',
  onSubmit,
  isSubmitting,
}: RefundFormProps) {
  const form = useForm<RefundFormValues>({
    resolver: zodResolver(refundFormSchema) as Resolver<RefundFormValues>,
    defaultValues: {
      paymentId: defaultPaymentId,
      amount: 0,
      currency: defaultCurrency,
      reason: '',
    },
  })

  const [confirmOpen, setConfirmOpen] = React.useState(false)
  const [pending, setPending] = React.useState<RefundFormValues | null>(null)

  return (
    <>
      <form
        className="max-w-md space-y-4"
        onSubmit={form.handleSubmit((vals: RefundFormValues) => {
          setPending(vals)
          setConfirmOpen(true)
        })}
      >
        <div className="space-y-2">
          <Label htmlFor="paymentId">Payment ID</Label>
          <Input id="paymentId" {...form.register('paymentId')} />
          {form.formState.errors.paymentId ? (
            <p className="text-destructive text-xs">
              {form.formState.errors.paymentId.message}
            </p>
          ) : null}
        </div>
        <div className="space-y-2">
          <Label htmlFor="amount">Amount (minor units, e.g. cents)</Label>
          <Input id="amount" type="number" inputMode="numeric" {...form.register('amount')} />
          {form.formState.errors.amount ? (
            <p className="text-destructive text-xs">{form.formState.errors.amount.message}</p>
          ) : null}
        </div>
        <div className="space-y-2">
          <Label htmlFor="currency">Currency</Label>
          <Input id="currency" maxLength={3} {...form.register('currency')} />
          {form.formState.errors.currency ? (
            <p className="text-destructive text-xs">{form.formState.errors.currency.message}</p>
          ) : null}
        </div>
        <div className="space-y-2">
          <Label htmlFor="reason">Reason (optional)</Label>
          <Input id="reason" {...form.register('reason')} />
        </div>
        <Button type="submit" disabled={isSubmitting}>
          Review refund
        </Button>
      </form>

      <Dialog open={confirmOpen} onOpenChange={setConfirmOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Confirm refund</DialogTitle>
            <DialogDescription>
              This issues a refund against the payment in PayFlow. You cannot undo this from the
              dashboard.
            </DialogDescription>
          </DialogHeader>
          {pending ? (
            <ul className="text-muted-foreground text-sm">
              <li>
                <span className="text-foreground font-medium">Payment:</span> {pending.paymentId}
              </li>
              <li>
                <span className="text-foreground font-medium">Amount:</span> {pending.amount}{' '}
                {pending.currency}
              </li>
              {pending.reason ? (
                <li>
                  <span className="text-foreground font-medium">Reason:</span> {pending.reason}
                </li>
              ) : null}
            </ul>
          ) : null}
          <DialogFooter className="gap-2 sm:gap-0">
            <Button type="button" variant="outline" onClick={() => setConfirmOpen(false)}>
              Cancel
            </Button>
            <Button
              type="button"
              disabled={isSubmitting || !pending}
              onClick={async () => {
                if (!pending) return
                try {
                  await onSubmit(pending)
                  setConfirmOpen(false)
                  setPending(null)
                  form.reset({
                    paymentId: pending.paymentId,
                    amount: 0,
                    currency: pending.currency,
                    reason: '',
                  })
                } catch {
                  /* parent toasts */
                }
              }}
            >
              Submit refund
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  )
}
