import { Outlet } from 'react-router-dom'

import { Sidebar } from '@/components/layout/Sidebar'
import { useAuthStore, selectIsAuthenticated } from '@/stores/auth-store'

export function AppLayout() {
  const apiKey = useAuthStore((s) => s.apiKey)
  const isAuthed = useAuthStore(selectIsAuthenticated)

  return (
    <div className="bg-background flex min-h-svh w-full">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="border-border flex h-14 items-center justify-between border-b px-6">
          <h1 className="text-sm font-medium">Merchant console</h1>
          <div className="text-muted-foreground text-xs">
            {isAuthed && apiKey ? (
              <span title={apiKey}>Key: {apiKey.slice(0, 10)}…</span>
            ) : (
              <span>No API key</span>
            )}
          </div>
        </header>
        <main className="flex-1 overflow-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
