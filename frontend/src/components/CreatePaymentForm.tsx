import { zodResolver } from '@hookform/resolvers/zod'
import { useForm, type Resolver } from 'react-hook-form'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  paymentFormSchema,
  type PaymentFormValues,
} from '@/lib/payment-form-schema'

export type CreatePaymentFormProps = {
  onSubmit: (values: PaymentFormValues) => Promise<void>
  isSubmitting?: boolean
}

const currentYear = new Date().getFullYear()
const years = Array.from({ length: 10 }, (_, i) => currentYear + i)
const months = Array.from({ length: 12 }, (_, i) => i + 1)

export function CreatePaymentForm({
  onSubmit,
  isSubmitting,
}: CreatePaymentFormProps) {
  const form = useForm<PaymentFormValues>({
    resolver: zodResolver(paymentFormSchema) as Resolver<PaymentFormValues>,
    defaultValues: {
      amount: 0,
      currency: 'USD',
      cardNumber: '',
      expMonth: 1,
      expYear: currentYear,
      cvc: '',
    },
  })

  return (
    <form
      className="max-w-md space-y-6"
      onSubmit={form.handleSubmit(onSubmit)}
    >
      <div className="space-y-2">
        <Label htmlFor="amount">Amount (minor units, e.g. cents)</Label>
        <Input
          id="amount"
          type="number"
          inputMode="numeric"
          placeholder="1000"
          {...form.register('amount')}
        />
        {form.formState.errors.amount ? (
          <p className="text-destructive text-xs">
            {form.formState.errors.amount.message}
          </p>
        ) : null}
      </div>

      <div className="space-y-2">
        <Label htmlFor="currency">Currency</Label>
        <Select
          value={form.watch('currency')}
          onValueChange={(val) => form.setValue('currency', val as 'USD' | 'EUR' | 'GBP')}
        >
          <SelectTrigger id="currency">
            <SelectValue placeholder="Select currency" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="USD">USD - US Dollar</SelectItem>
            <SelectItem value="EUR">EUR - Euro</SelectItem>
            <SelectItem value="GBP">GBP - British Pound</SelectItem>
          </SelectContent>
        </Select>
        {form.formState.errors.currency ? (
          <p className="text-destructive text-xs">
            {form.formState.errors.currency.message}
          </p>
        ) : null}
      </div>

      <div className="space-y-2">
        <Label htmlFor="cardNumber">Card Number</Label>
        <Input
          id="cardNumber"
          inputMode="numeric"
          placeholder="4111111111111111"
          maxLength={16}
          {...form.register('cardNumber')}
        />
        {form.formState.errors.cardNumber ? (
          <p className="text-destructive text-xs">
            {form.formState.errors.cardNumber.message}
          </p>
        ) : null}
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label htmlFor="expMonth">Expiry Month</Label>
          <Select
            value={String(form.watch('expMonth'))}
            onValueChange={(val) => form.setValue('expMonth', parseInt(val || '1', 10))}
          >
            <SelectTrigger id="expMonth">
              <SelectValue placeholder="Month" />
            </SelectTrigger>
            <SelectContent>
              {months.map((m) => (
                <SelectItem key={m} value={String(m)}>
                  {m.toString().padStart(2, '0')}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          {form.formState.errors.expMonth ? (
            <p className="text-destructive text-xs">
              {form.formState.errors.expMonth.message}
            </p>
          ) : null}
        </div>

        <div className="space-y-2">
          <Label htmlFor="expYear">Expiry Year</Label>
          <Select
            value={String(form.watch('expYear'))}
            onValueChange={(val) => form.setValue('expYear', parseInt(val || String(currentYear), 10))}
          >
            <SelectTrigger id="expYear">
              <SelectValue placeholder="Year" />
            </SelectTrigger>
            <SelectContent>
              {years.map((y) => (
                <SelectItem key={y} value={String(y)}>
                  {y}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          {form.formState.errors.expYear ? (
            <p className="text-destructive text-xs">
              {form.formState.errors.expYear.message}
            </p>
          ) : null}
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="cvc">CVC</Label>
        <Input
          id="cvc"
          inputMode="numeric"
          placeholder="123"
          maxLength={4}
          {...form.register('cvc')}
        />
        {form.formState.errors.cvc ? (
          <p className="text-destructive text-xs">
            {form.formState.errors.cvc.message}
          </p>
        ) : null}
      </div>

      <Button type="submit" disabled={isSubmitting}>
        {isSubmitting ? 'Creating...' : 'Create Payment'}
      </Button>
    </form>
  )
}