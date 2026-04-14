import { beforeEach, describe, expect, it } from 'vitest'

import { selectIsAuthenticated, useAuthStore } from '@/stores/auth-store'

describe('auth store', () => {
  beforeEach(() => {
    useAuthStore.setState({ apiKey: null })
    localStorage.removeItem('payflow-auth')
  })

  it('starts unauthenticated when api key cleared', () => {
    useAuthStore.setState({ apiKey: null })
    expect(selectIsAuthenticated(useAuthStore.getState())).toBe(false)
  })

  it('is authenticated when api key is non-empty', () => {
    useAuthStore.getState().setApiKey('sk_test_abc')
    expect(selectIsAuthenticated(useAuthStore.getState())).toBe(true)
    expect(useAuthStore.getState().apiKey).toBe('sk_test_abc')
  })

  it('clears api key', () => {
    useAuthStore.getState().setApiKey('sk_test_abc')
    useAuthStore.getState().clearApiKey()
    expect(useAuthStore.getState().apiKey).toBeNull()
    expect(selectIsAuthenticated(useAuthStore.getState())).toBe(false)
  })
})
