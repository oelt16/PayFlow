import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  deactivateMerchant,
  getMerchantMe,
  registerMerchant,
  rotateApiKey,
} from '@/services/merchants'

export const merchantKeys = {
  me: ['merchant', 'me'] as const,
}

export function useMerchantMe(enabled: boolean) {
  return useQuery({
    queryKey: merchantKeys.me,
    queryFn: getMerchantMe,
    enabled,
    staleTime: 60_000,
  })
}

export function useRegisterMerchant() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: registerMerchant,
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: merchantKeys.me })
    },
  })
}

export function useRotateApiKey() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: rotateApiKey,
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: merchantKeys.me })
    },
  })
}

export function useDeactivateMerchant() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: deactivateMerchant,
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: merchantKeys.me })
    },
  })
}
