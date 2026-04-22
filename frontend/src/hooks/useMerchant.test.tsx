import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook } from '@testing-library/react'
import { createElement, type ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useRotateApiKey } from '@/hooks/useMerchant'
import { useAuthStore } from '@/stores/auth-store'

vi.mock('@/services/merchants', () => ({
  deactivateMerchant: vi.fn(),
  getMerchantMe: vi.fn(),
  registerMerchant: vi.fn(),
  rotateApiKey: vi.fn(() => Promise.resolve({ apiKey: 'sk_test_after_rotation' })),
}))

describe('useRotateApiKey', () => {
  let queryClient: QueryClient

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    })
    useAuthStore.setState({ apiKey: 'sk_test_before_rotation' })
  })

  function wrapper({ children }: { children: ReactNode }) {
    return createElement(QueryClientProvider, { client: queryClient }, children)
  }

  it('sets the new api key on the store before invalidateQueries runs', async () => {
    vi.spyOn(queryClient, 'invalidateQueries').mockImplementation(async () => {
      expect(useAuthStore.getState().apiKey).toBe('sk_test_after_rotation')
      return Promise.resolve()
    })

    const { result } = renderHook(() => useRotateApiKey(), { wrapper })

    await result.current.mutateAsync()

    expect(useAuthStore.getState().apiKey).toBe('sk_test_after_rotation')
  })
})
