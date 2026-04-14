import type { InternalAxiosRequestConfig } from 'axios'
import { beforeEach, describe, expect, it } from 'vitest'

import { apiClient } from '@/services/api-client'
import { useAuthStore } from '@/stores/auth-store'

describe('apiClient auth header', () => {
  beforeEach(() => {
    useAuthStore.setState({ apiKey: null })
  })

  it('does not set Authorization when api key is absent', async () => {
    await apiClient.get('/v1/payments', {
      adapter: async (config) => {
        expect(config.headers?.Authorization).toBeUndefined()
        return {
          data: { content: [], totalElements: 0, page: 0, size: 20 },
          status: 200,
          statusText: 'OK',
          headers: {},
          config: config as InternalAxiosRequestConfig,
        }
      },
    })
  })

  it('sets Bearer Authorization when api key is present', async () => {
    useAuthStore.getState().setApiKey('sk_test_dev')
    await apiClient.get('/v1/payments', {
      adapter: async (config) => {
        expect(config.headers?.Authorization).toBe('Bearer sk_test_dev')
        return {
          data: { content: [], totalElements: 0, page: 0, size: 20 },
          status: 200,
          statusText: 'OK',
          headers: {},
          config: config as InternalAxiosRequestConfig,
        }
      },
    })
  })
})
