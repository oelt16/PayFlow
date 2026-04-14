/**
 * Amounts from the API are in minor units (e.g. cents).
 */
export function formatMinorUnits(amountMinor: number, currency: string): string {
  const code = currency.toUpperCase()
  const divisor = 10 ** minorUnitDigits(code)
  const major = amountMinor / divisor
  try {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: code,
    }).format(major)
  } catch {
    return `${major.toFixed(2)} ${code}`
  }
}

function minorUnitDigits(currency: string): number {
  const zeroDecimal = new Set(['JPY', 'KRW', 'VND'])
  if (zeroDecimal.has(currency)) return 0
  return 2
}

export function formatIsoDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString()
}

export function maskApiKey(raw: string): string {
  const t = raw.trim()
  if (t.length <= 14) return '••••••••'
  const start = t.slice(0, 12)
  const end = t.slice(-4)
  return `${start}••••${end}`
}
