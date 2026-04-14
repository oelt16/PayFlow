import { Link } from 'react-router-dom'

import { buttonVariants } from '@/components/ui/button'
import { cn } from '@/lib/utils'

export function NotFoundPage() {
  return (
    <div className="flex min-h-[40vh] flex-col items-center justify-center gap-4 text-center">
      <h2 className="text-2xl font-semibold">Page not found</h2>
      <p className="text-muted-foreground text-sm">That route does not exist in the PayFlow dashboard.</p>
      <Link to="/" className={cn(buttonVariants())}>
        Go home
      </Link>
    </div>
  )
}
