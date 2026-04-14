import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  deactivateWebhook,
  listDeliveries,
  listWebhooks,
  registerWebhook,
} from '@/services/webhooks'

export const webhooksKeys = {
  all: ['webhooks'] as const,
  list: () => [...webhooksKeys.all, 'list'] as const,
  deliveries: (id: string) => [...webhooksKeys.all, 'deliveries', id] as const,
}

export function useWebhooksList(enabled = true) {
  return useQuery({
    queryKey: webhooksKeys.list(),
    queryFn: listWebhooks,
    enabled,
    staleTime: 15_000,
  })
}

export function useWebhookDeliveries(webhookId: string | undefined, refetchIntervalMs?: number) {
  return useQuery({
    queryKey: webhooksKeys.deliveries(webhookId ?? ''),
    queryFn: () => listDeliveries(webhookId!),
    enabled: Boolean(webhookId),
    refetchInterval: refetchIntervalMs,
  })
}

export function useRegisterWebhook() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: { url: string; events: string[] }) => registerWebhook(body),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: webhooksKeys.all })
    },
  })
}

export function useDeactivateWebhook() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => deactivateWebhook(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: webhooksKeys.all })
    },
  })
}
