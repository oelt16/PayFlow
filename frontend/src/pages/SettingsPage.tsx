import { useState } from 'react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Separator } from '@/components/ui/separator'
import { useMerchantMe, useRegisterMerchant, useRotateApiKey } from '@/hooks/useMerchant'
import { toastApiError } from '@/lib/toast-error'
import { formatIsoDate, maskApiKey } from '@/lib/format'
import { selectIsAuthenticated, useAuthStore } from '@/stores/auth-store'

export function SettingsPage() {
  const apiKey = useAuthStore((s) => s.apiKey)
  const setApiKey = useAuthStore((s) => s.setApiKey)
  const clearApiKey = useAuthStore((s) => s.clearApiKey)
  const isAuthed = useAuthStore(selectIsAuthenticated)

  const [keyInput, setKeyInput] = useState('')
  const [regName, setRegName] = useState('')
  const [regEmail, setRegEmail] = useState('')

  const { data: me, isLoading: meLoading, error: meError } = useMerchantMe(isAuthed)
  const registerMut = useRegisterMerchant()
  const rotateMut = useRotateApiKey()

  return (
    <div className="mx-auto max-w-2xl space-y-8">
      <div>
        <h2 className="text-2xl font-semibold tracking-tight">Settings</h2>
        <p className="text-muted-foreground text-sm">
          Connect an API key from merchant registration, or register a new merchant. Keys are stored
          in this browser only.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>API key</CardTitle>
          <CardDescription>
            Paste <code className="text-xs">sk_test_…</code> from registration or rotation. Sent as{' '}
            <code className="text-xs">Authorization: Bearer</code> on every request.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {isAuthed && apiKey ? (
            <p className="text-sm">
              Current key: <span className="font-mono text-xs">{maskApiKey(apiKey)}</span>
            </p>
          ) : null}
          <div className="flex flex-col gap-2 sm:flex-row sm:items-end">
            <div className="min-w-0 flex-1 space-y-2">
              <Label htmlFor="apiKey">API key</Label>
              <Input
                id="apiKey"
                type="password"
                autoComplete="off"
                value={keyInput}
                onChange={(e) => setKeyInput(e.target.value)}
                placeholder="sk_test_…"
              />
            </div>
            <Button
              type="button"
              onClick={() => {
                const t = keyInput.trim()
                if (!t) return
                setApiKey(t)
                setKeyInput('')
              }}
            >
              Save key
            </Button>
            <Button type="button" variant="outline" onClick={() => clearApiKey()}>
              Clear
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Register merchant</CardTitle>
          <CardDescription>
            Creates a merchant and returns a new API key (shown once). This call does not require an
            existing key.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="regName">Name</Label>
              <Input
                id="regName"
                value={regName}
                onChange={(e) => setRegName(e.target.value)}
                placeholder="Demo Shop"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="regEmail">Email</Label>
              <Input
                id="regEmail"
                type="email"
                value={regEmail}
                onChange={(e) => setRegEmail(e.target.value)}
                placeholder="you@example.com"
              />
            </div>
          </div>
          <Button
            type="button"
            disabled={registerMut.isPending}
            onClick={async () => {
              try {
                await registerMut.mutateAsync({
                  name: regName.trim(),
                  email: regEmail.trim(),
                })
                setRegName('')
                setRegEmail('')
              } catch (e) {
                toastApiError(e)
              }
            }}
          >
            Register and save key
          </Button>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Merchant profile</CardTitle>
          <CardDescription>Requires a valid API key (merchant-service).</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {!isAuthed ? (
            <p className="text-muted-foreground text-sm">Save an API key to load your profile.</p>
          ) : meLoading ? (
            <p className="text-muted-foreground text-sm">Loading…</p>
          ) : meError ? (
            <p className="text-destructive text-sm">
              Profile unavailable. Check the API key and that merchant-service is reachable.
            </p>
          ) : me ? (
            <dl className="grid gap-2 text-sm">
              <div className="flex justify-between gap-4">
                <dt className="text-muted-foreground">ID</dt>
                <dd className="font-mono text-xs">{me.id}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-muted-foreground">Name</dt>
                <dd>{me.name}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-muted-foreground">Email</dt>
                <dd>{me.email}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-muted-foreground">Active</dt>
                <dd>{me.active ? 'Yes' : 'No'}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-muted-foreground">Created</dt>
                <dd>{formatIsoDate(me.createdAt)}</dd>
              </div>
            </dl>
          ) : null}

          <Separator />

          <div className="flex flex-wrap gap-2">
            <Button
              type="button"
              variant="secondary"
              disabled={!isAuthed || rotateMut.isPending}
              onClick={async () => {
                try {
                  await rotateMut.mutateAsync()
                } catch (e) {
                  toastApiError(e)
                }
              }}
            >
              Rotate API key
            </Button>
          </div>
          <p className="text-muted-foreground text-xs">
            Rotating replaces your key everywhere; save the new key before leaving this page.
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
