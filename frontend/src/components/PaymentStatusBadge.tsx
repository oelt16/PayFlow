import { Badge } from '@/components/ui/badge'

const variantByStatus: Record<
  string,
  'default' | 'secondary' | 'destructive' | 'outline' | 'ghost'
> = {
  PENDING: 'secondary',
  CAPTURED: 'default',
  CANCELLED: 'destructive',
  REFUNDED: 'outline',
  PARTIAL_REFUND: 'outline',
  EXPIRED: 'ghost',
}

export type PaymentStatusBadgeProps = {
  status: string
  className?: string
}

export function PaymentStatusBadge({ status, className }: PaymentStatusBadgeProps) {
  const variant = variantByStatus[status] ?? 'outline'
  return (
    <Badge variant={variant} className={className}>
      {status}
    </Badge>
  )
}
