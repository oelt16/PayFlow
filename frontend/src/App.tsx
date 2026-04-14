import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Route, Routes } from 'react-router-dom'

import { AppLayout } from '@/components/layout/AppLayout'
import { RequireAuth } from '@/components/RequireAuth'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { OverviewPage } from '@/pages/OverviewPage'
import { PaymentDetailPage } from '@/pages/PaymentDetailPage'
import { PaymentsPage } from '@/pages/PaymentsPage'
import { RefundPage } from '@/pages/RefundPage'
import { SettingsPage } from '@/pages/SettingsPage'
import { WebhooksPage } from '@/pages/WebhooksPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Routes>
        <Route path="/" element={<AppLayout />}>
          <Route path="settings" element={<SettingsPage />} />
          <Route element={<RequireAuth />}>
            <Route index element={<OverviewPage />} />
            <Route path="payments" element={<PaymentsPage />} />
            <Route path="payments/:id" element={<PaymentDetailPage />} />
            <Route path="refunds" element={<RefundPage />} />
            <Route path="webhooks" element={<WebhooksPage />} />
          </Route>
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </QueryClientProvider>
  )
}
