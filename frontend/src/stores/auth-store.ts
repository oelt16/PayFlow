import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type AuthState = {
  apiKey: string | null
  setApiKey: (key: string | null) => void
  clearApiKey: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      apiKey: null,
      setApiKey: (apiKey) => set({ apiKey }),
      clearApiKey: () => set({ apiKey: null }),
    }),
    { name: 'payflow-auth' },
  ),
)

export function selectIsAuthenticated(state: AuthState): boolean {
  return Boolean(state.apiKey?.trim())
}
