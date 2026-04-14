import axios from 'axios'

import { useAuthStore } from '@/stores/auth-store'

/** Single seam for dev proxy and K8s Ingress; never hard-code host or port. */
export const API_BASE = '/api'

export const apiClient = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.request.use((config) => {
  const key = useAuthStore.getState().apiKey
  if (key?.trim()) {
    config.headers.Authorization = `Bearer ${key.trim()}`
  }
  return config
})
