import { useState } from 'react'

import { WebhookDeliveryLog } from '@/components/WebhookDeliveryLog'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { useDeactivateWebhook, useRegisterWebhook, useWebhooksList } from '@/hooks/useWebhooks'
import { toastApiError } from '@/lib/toast-error'
import { WEBHOOK_EVENT_TYPES } from '@/types/webhook'
import { selectIsAuthenticated, useAuthStore } from '@/stores/auth-store'

export function WebhooksPage() {
  const isAuthed = useAuthStore(selectIsAuthenticated)
  const { data, isLoading, isError } = useWebhooksList(isAuthed)
  const registerMut = useRegisterWebhook()
  const deactivateMut = useDeactivateWebhook()

  const [url, setUrl] = useState('https://example.com/webhooks')
  const [selectedEvents, setSelectedEvents] = useState<Set<string>>(
    () => new Set(['payment.captured']),
  )
  const [expandedId, setExpandedId] = useState<string | null>(null)

  if (!isAuthed) return null

  const toggleEvent = (e: string, checked: boolean) => {
    setSelectedEvents((prev) => {
      const next = new Set(prev)
      if (checked) next.add(e)
      else next.delete(e)
      return next
    })
  }

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-2xl font-semibold tracking-tight">Webhooks</h2>
        <p className="text-muted-foreground text-sm">
          Register HTTPS endpoints. Delivery log polls every 5 seconds when expanded.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Add endpoint</CardTitle>
          <CardDescription>URL must be HTTPS (enforced by webhook-service).</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="whUrl">URL</Label>
            <Input id="whUrl" value={url} onChange={(e) => setUrl(e.target.value)} />
          </div>
          <div className="space-y-2">
            <Label>Events</Label>
            <div className="grid gap-2 sm:grid-cols-2">
              {WEBHOOK_EVENT_TYPES.map((ev) => (
                <label key={ev} className="flex items-center gap-2 text-sm">
                  <Checkbox
                    checked={selectedEvents.has(ev)}
                    onCheckedChange={(c) => toggleEvent(ev, c === true)}
                  />
                  <span className="font-mono text-xs">{ev}</span>
                </label>
              ))}
            </div>
          </div>
          <Button
            type="button"
            disabled={registerMut.isPending || selectedEvents.size === 0}
            onClick={async () => {
              try {
                const res = await registerMut.mutateAsync({
                  url: url.trim(),
                  events: Array.from(selectedEvents),
                })
                window.alert(`Webhook created. Secret (save once): ${res.secret}`)
              } catch (e) {
                toastApiError(e)
              }
            }}
          >
            Register webhook
          </Button>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Endpoints</CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <Skeleton className="h-32 w-full" />
          ) : isError ? (
            <p className="text-destructive text-sm">Could not load webhooks.</p>
          ) : !data?.content.length ? (
            <p className="text-muted-foreground text-sm">No webhooks registered.</p>
          ) : (
            <div className="space-y-4">
              {data.content.map((w) => (
                <Card key={w.id}>
                  <CardHeader className="pb-2">
                    <div className="flex flex-wrap items-start justify-between gap-2">
                      <div>
                        <CardTitle className="font-mono text-sm">{w.id}</CardTitle>
                        <p className="text-muted-foreground mt-1 break-all text-xs">{w.url}</p>
                        <p className="text-muted-foreground mt-1 text-xs">
                          {w.active ? 'Active' : 'Inactive'} · {w.events.join(', ')}
                        </p>
                      </div>
                      <div className="flex gap-2">
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          onClick={() =>
                            setExpandedId((id) => (id === w.id ? null : w.id))
                          }
                        >
                          {expandedId === w.id ? 'Hide deliveries' : 'Deliveries'}
                        </Button>
                        <Button
                          type="button"
                          variant="destructive"
                          size="sm"
                          disabled={deactivateMut.isPending}
                          onClick={async () => {
                            if (!window.confirm('Deactivate this webhook endpoint?')) return
                            try {
                              await deactivateMut.mutateAsync(w.id)
                            } catch (e) {
                              toastApiError(e)
                            }
                          }}
                        >
                          Delete
                        </Button>
                      </div>
                    </div>
                  </CardHeader>
                  {expandedId === w.id ? (
                    <CardContent>
                      <WebhookDeliveryLog webhookId={w.id} />
                    </CardContent>
                  ) : null}
                </Card>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
