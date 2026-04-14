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
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: paymentsKeys.all })
    },
  })
}
