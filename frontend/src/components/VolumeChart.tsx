import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

export type VolumeChartPoint = {
  day: string
  volumeMinor: number
}

export type VolumeChartProps = {
  data: VolumeChartPoint[]
  currency: string
}

export function VolumeChart({ data, currency }: VolumeChartProps) {
  if (data.length === 0) {
    return (
      <p className="text-muted-foreground py-12 text-center text-sm">
        No payment volume in the last 30 days.
      </p>
    )
  }

  return (
    <div className="h-[280px] w-full">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
          <defs>
            <linearGradient id="payflowVolume" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="var(--color-chart-1)" stopOpacity={0.35} />
              <stop offset="95%" stopColor="var(--color-chart-1)" stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
          <XAxis dataKey="day" tick={{ fontSize: 11 }} className="text-muted-foreground" />
          <YAxis
            tick={{ fontSize: 11 }}
            className="text-muted-foreground"
            tickFormatter={(v) => `${(v / 100).toFixed(0)}`}
          />
          <Tooltip
            formatter={(value) => {
              const v = typeof value === 'number' ? value : 0
              return [`${(v / 100).toFixed(2)} ${currency}`, 'Volume']
            }}
            labelFormatter={(label) => String(label)}
          />
          <Area
            type="monotone"
            dataKey="volumeMinor"
            stroke="var(--color-chart-1)"
            fillOpacity={1}
            fill="url(#payflowVolume)"
            strokeWidth={2}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}
