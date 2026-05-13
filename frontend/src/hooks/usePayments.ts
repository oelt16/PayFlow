import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  cancelPayment,
  capturePayment,
  createPayment,
  createRefund,
  getPayment,
  listPayments,
  listRefunds,
} from '@/services/payments'
import type { CreatePaymentBody } from '@/services/payments'

export const paymentsKeys = {
  all: ['payments'] as const,
  list: (page: number, size: number, status?: string) =>
    [...paymentsKeys.all, 'list', page, size, status ?? ''] as const,
  detail: (id: string) => [...paymentsKeys.all, 'detail', id] as const,
  refunds: (id: string) => [...paymentsKeys.all, 'refunds', id] as const,
}

export function usePaymentsList(
  page: number,
  size: number,
  status?: string,
  enabled = true,
) {
  return useQuery({
    queryKey: paymentsKeys.list(page, size, status),
    queryFn: () => listPayments({ page, size, status }),
    enabled,
    staleTime: 30_000,
  })
}

export function usePayment(id: string | undefined, enabled = true) {
  return useQuery({
    queryKey: paymentsKeys.detail(id ?? ''),
    queryFn: () => getPayment(id!),
    enabled: Boolean(id) && enabled,
  })
}

export function useRefunds(paymentId: string | undefined, enabled = true) {
  return useQuery({
    queryKey: paymentsKeys.refunds(paymentId ?? ''),
    queryFn: () => listRefunds(paymentId!),
    enabled: Boolean(paymentId) && enabled,
  })
}

export function useCapturePayment() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => capturePayment(id),
    onSuccess: (_data, id) => {
      void qc.invalidateQueries({ queryKey: paymentsKeys.all })
      void qc.invalidateQueries({ queryKey: paymentsKeys.detail(id) })
    },
  })
}

export function useCancelPayment() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      cancelPayment(id, reason),
    onSuccess: (_data, { id }) => {
      void qc.invalidateQueries({ queryKey: paymentsKeys.all })
      void qc.invalidateQueries({ queryKey: paymentsKeys.detail(id) })
    },
  })
}

export function useCreateRefund() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({
      paymentId,
      amount,
      currency,
      reason,
    }: {
      paymentId: string
      amount: number
      currency: string
      reason?: string | null
    }) => createRefund(paymentId, { amount, currency, reason }),
    onSuccess: (_data, { paymentId }) => {
      void qc.invalidateQueries({ queryKey: paymentsKeys.all })
      void qc.invalidateQueries({ queryKey: paymentsKeys.detail(paymentId) })
      void qc.invalidateQueries({ queryKey: paymentsKeys.refunds(paymentId) })
    },
  })
}

export function useCreatePayment() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: CreatePaymentBody) => createPayment(body),
    onMutate: async (newPayment) => {
      // Cancel any outgoing refetches so they don't overwrite our optimistic update
      await qc.cancelQueries({ queryKey: paymentsKeys.all })

      // Snapshot the previous value for rollback
      const previousPayments = qc.getQueryData(paymentsKeys.list(0, 20, ''))

      // Optimistically update the payments list
      const optimisticPayment = {
        id: `temp_${Date.now()}`,
        amount: newPayment.amount,
        currency: newPayment.currency,
        status: 'PENDING',
        description: null,
        clientSecret: null,
        metadata: newPayment.metadata ?? {},
        card: {
          last4: newPayment.card.number.slice(-4),
          brand: 'Unknown',
          expMonth: newPayment.card.expMonth,
          expYear: newPayment.card.expYear,
        },
        createdAt: new Date().toISOString(),
        expiresAt: null,
        capturedAt: null,
        cancelledAt: null,
        totalRefunded: 0,
        amountRefunded: 0,
      }

      qc.setQueryData<{ content: unknown[]; totalElements: number }>(
        paymentsKeys.list(0, 20, ''),
        (old) => {
          if (!old) return old
          return {
            ...old,
            content: [optimisticPayment, ...old.content],
            totalElements: old.totalElements + 1,
          }
        },
      )

      // Return context with previous value for rollback
      return { previousPayments }
    },
    onError: (_err, _newPayment, context) => {
      // Rollback to previous value on error
      if (context?.previousPayments) {
        qc.setQueryData(paymentsKeys.list(0, 20, ''), context.previousPayments)
      }
    },
    onSettled: () => {
      // Invalidate to ensure we have the correct data from server
      void qc.invalidateQueries({ queryKey: paymentsKeys.all })
    },
  })
}
