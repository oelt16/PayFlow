import { useQuery } from '@tanstack/react-query'

import { listPayments } from '@/services/payments'

import { paymentsKeys } from './usePayments'

export function useOverviewPayments(enabled: boolean) {
  return useQuery({
    queryKey: [...paymentsKeys.all, 'overview'] as const,
    enabled,
    staleTime: 60_000,
    queryFn: async () => {
      const size = 200
      const maxPages = 10
      let page = 0
      const all = []
      let totalElements = 0
      while (page < maxPages) {
        const r = await listPayments({ page, size })
        totalElements = r.totalElements
        all.push(...r.content)
        if (r.content.length === 0 || all.length >= totalElements) break
        page += 1
      }
      return { payments: all, totalElements }
    },
  })
}
