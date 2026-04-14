import { Link } from 'react-router-dom'

import { PaymentStatusBadge } from '@/components/PaymentStatusBadge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { formatIsoDate, formatMinorUnits } from '@/lib/format'
import type { PaymentResponse } from '@/types/payment'

export type PaymentTableProps = {
  payments: PaymentResponse[]
}

export function PaymentTable({ payments }: PaymentTableProps) {
  if (payments.length === 0) {
    return (
      <p className="text-muted-foreground py-8 text-center text-sm">No payments yet.</p>
    )
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>ID</TableHead>
          <TableHead>Amount</TableHead>
          <TableHead>Status</TableHead>
          <TableHead>Created</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {payments.map((p) => (
          <TableRow key={p.id}>
            <TableCell className="font-mono text-xs">
              <Link
                to={`/payments/${encodeURIComponent(p.id)}`}
                className="text-primary hover:underline"
              >
                {p.id}
              </Link>
            </TableCell>
            <TableCell>{formatMinorUnits(p.amount, p.currency)}</TableCell>
            <TableCell>
              <PaymentStatusBadge status={p.status} />
            </TableCell>
            <TableCell className="text-muted-foreground text-sm">
              {formatIsoDate(p.createdAt)}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
