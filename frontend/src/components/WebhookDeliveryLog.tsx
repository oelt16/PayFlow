import { useWebhookDeliveries } from '@/hooks/useWebhooks'
import { formatIsoDate } from '@/lib/format'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Skeleton } from '@/components/ui/skeleton'

export type WebhookDeliveryLogProps = {
  webhookId: string
}

export function WebhookDeliveryLog({ webhookId }: WebhookDeliveryLogProps) {
  const { data, isLoading, isError, dataUpdatedAt } = useWebhookDeliveries(
    webhookId,
    5000,
  )

  if (isLoading) {
    return (
      <div className="space-y-2 py-2">
        <Skeleton className="h-8 w-full" />
        <Skeleton className="h-8 w-full" />
        <Skeleton className="h-8 w-full" />
      </div>
    )
  }

  if (isError) {
    return (
      <p className="text-destructive text-sm">Could not load deliveries for this endpoint.</p>
    )
  }

  const rows = data?.data ?? []

  return (
    <div className="space-y-2">
      <p className="text-muted-foreground text-xs">
        Refreshes every 5s
        {dataUpdatedAt ? ` · last update ${new Date(dataUpdatedAt).toLocaleTimeString()}` : null}
      </p>
      {rows.length === 0 ? (
        <p className="text-muted-foreground text-sm">No delivery attempts yet.</p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Event</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Attempts</TableHead>
              <TableHead>Last attempt</TableHead>
              <TableHead>Error</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((d) => (
              <TableRow key={d.id}>
                <TableCell className="font-mono text-xs">{d.eventType}</TableCell>
                <TableCell>{d.status}</TableCell>
                <TableCell>{d.attempts}</TableCell>
                <TableCell className="text-muted-foreground text-sm">
                  {formatIsoDate(d.lastAttemptAt ?? undefined)}
                </TableCell>
                <TableCell className="max-w-[200px] truncate text-xs" title={d.lastError ?? ''}>
                  {d.lastError ?? '—'}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  )
}
