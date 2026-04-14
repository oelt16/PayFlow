import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { selectIsAuthenticated, useAuthStore } from '@/stores/auth-store'

export function RequireAuth() {
  const location = useLocation()
  const isAuthed = useAuthStore(selectIsAuthenticated)

  if (!isAuthed) {
    return <Navigate to="/settings" replace state={{ from: location.pathname }} />
  }

  return <Outlet />
}
