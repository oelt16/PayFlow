import { useNavigate } from 'react-router-dom'

import { CreatePaymentForm } from '@/components/CreatePaymentForm'
import { toastApiError } from '@/lib/toast-error'
import { useCreatePayment } from '@/hooks/usePayments'
import type { PaymentFormValues } from '@/lib/payment-form-schema'
import { paymentFormValuesToCreatePaymentBody } from '@/lib/payment-form-schema'

export function CreatePaymentPage() {
  const navigate = useNavigate()
  const createPayment = useCreatePayment()

  const handleSubmit = async (values: PaymentFormValues) => {
    try {
      const body = paymentFormValuesToCreatePaymentBody(values)
      const result = await createPayment.mutateAsync(body)
      navigate(`/payments/${result.id}`)
    } catch (error) {
      toastApiError(error, 'Failed to create payment')
    }
  }

  return (
    <div className="mx-auto max-w-2xl py-8">
      <h1 className="mb-6 text-2xl font-semibold">Create Payment</h1>
      <CreatePaymentForm
        onSubmit={handleSubmit}
        isSubmitting={createPayment.isPending}
      />
    </div>
  )
}