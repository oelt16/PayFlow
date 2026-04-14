import { useSearchParams } from 'react-router-dom'
import { toast } from 'sonner'

import { RefundForm } from '@/components/RefundForm'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useCreateRefund } from '@/hooks/usePayments'
import { toastApiError } from '@/lib/toast-error'
import type { RefundFormValues } from '@/lib/refund-form-schema'
import { selectIsAuthenticated, useAuthStore } from '@/stores/auth-store'

export function RefundPage() {
  const isAuthed = useAuthStore(selectIsAuthenticated)
  const [params] = useSearchParams()
  const defaultPaymentId = params.get('paymentId') ?? ''
  const defaultCurrency = params.get('currency') ?? 'USD'

  const createRefund = useCreateRefund()

  if (!isAuthed) return null

  const onSubmit = async (values: RefundFormValues) => {
    try {
      await createRefund.mutateAsync({
        paymentId: values.paymentId.trim(),
        amount: values.amount,
        currency: values.currency,
        reason: values.reason?.trim() || null,
      })
      toast.success('Refund created')
    } catch (e) {
      toastApiError(e)
      throw e
    }
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <h2 className="text-2xl font-semibold tracking-tight">Issue refund</h2>
        <p className="text-muted-foreground text-sm">
          Amount is in minor units (same as payment-service). Currency must match the payment.
        </p>
      </div>
      <Card>
        <CardHeader>
          <CardTitle>Refund form</CardTitle>
          <CardDescription>Validated with Zod before submit.</CardDescription>
        </CardHeader>
        <CardContent>
          <RefundForm
            defaultPaymentId={defaultPaymentId}
            defaultCurrency={defaultCurrency}
            onSubmit={onSubmit}
            isSubmitting={createRefund.isPending}
          />
        </CardContent>
      </Card>
    </div>
  )
}
