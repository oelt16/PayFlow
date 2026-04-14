import {
  paymentListResponseSchema,
  paymentResponseSchema,
  refundListResponseSchema,
  refundResponseSchema,
} from '@/types/payment'

import { apiClient } from './api-client'

export type CreatePaymentCard = {
  number: string
  expMonth: number
  expYear: number
  cvc: string
}

export type CreatePaymentBody = {
  amount: number
  currency: string
  description?: string
  card: CreatePaymentCard
  metadata?: Record<string, string>
}

export async function listPayments(params: {
  page?: number
  size?: number
  status?: string
}) {
  const { data } = await apiClient.get('/v1/payments', { params })
  return paymentListResponseSchema.parse(data)
}

export async function getPayment(id: string) {
  const { data } = await apiClient.get(`/v1/payments/${id}`)
  return paymentResponseSchema.parse(data)
}

export async function createPayment(body: CreatePaymentBody) {
  const { data } = await apiClient.post('/v1/payments', body)
  return paymentResponseSchema.parse(data)
}

export async function capturePayment(id: string) {
  const { data } = await apiClient.post(`/v1/payments/${id}/capture`)
  return paymentResponseSchema.parse(data)
}

export async function cancelPayment(id: string, reason?: string) {
  const body = reason?.trim() ? { reason: reason.trim() } : {}
  const { data } = await apiClient.post(`/v1/payments/${id}/cancel`, body)
  return paymentResponseSchema.parse(data)
}

export async function createRefund(
  paymentId: string,
  body: { amount: number; currency: string; reason?: string | null },
) {
  const { data } = await apiClient.post(`/v1/payments/${paymentId}/refunds`, body)
  return refundResponseSchema.parse(data)
}

export async function listRefunds(paymentId: string) {
  const { data } = await apiClient.get(`/v1/payments/${paymentId}/refunds`)
  return refundListResponseSchema.parse(data)
}
