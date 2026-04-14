import { NavLink } from 'react-router-dom'

import { cn } from '@/lib/utils'

const links = [
  { to: '/', label: 'Overview' },
  { to: '/payments', label: 'Payments' },
  { to: '/refunds', label: 'Refunds' },
  { to: '/webhooks', label: 'Webhooks' },
  { to: '/settings', label: 'Settings' },
] as const

export function Sidebar() {
  return (
    <aside className="bg-sidebar text-sidebar-foreground flex w-52 shrink-0 flex-col border-r border-sidebar-border">
      <div className="border-b border-sidebar-border px-4 py-4">
        <span className="text-sidebar-primary font-semibold tracking-tight">PayFlow</span>
        <p className="text-muted-foreground text-xs">Dashboard</p>
      </div>
      <nav className="flex flex-1 flex-col gap-0.5 p-2">
        {links.map((l) => (
          <NavLink
            key={l.to}
            to={l.to}
            end={l.to === '/'}
            className={({ isActive }) =>
              cn(
                'rounded-md px-3 py-2 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-sidebar-accent text-sidebar-accent-foreground'
                  : 'text-sidebar-foreground hover:bg-sidebar-accent/60',
              )
            }
          >
            {l.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
