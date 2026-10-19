import { useEffect, useRef, type ReactNode } from 'react'

type Props = { children: ReactNode; description: string; title?: string }

/** Blocking login dialog: keep keyboard focus inside until authentication succeeds. */
export function LoginOverlay({ children, description, title = '请扫码登录' }: Props) {
  const dialogRef = useRef<HTMLElement>(null)

  useEffect(() => {
    const previousFocus = document.activeElement as HTMLElement | null
    const oldOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    dialogRef.current?.focus()
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key !== 'Tab') return
      const dialog = dialogRef.current
      if (!dialog) return
      const elements = Array.from(dialog.querySelectorAll<HTMLElement>(
        'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex="0"]'
      )).filter(node => !node.hasAttribute('hidden'))
      if (!elements.length) {
        event.preventDefault()
        dialog.focus()
        return
      }
      const first = elements[0]
      const last = elements[elements.length - 1]
      if (event.shiftKey && (document.activeElement === first || document.activeElement === dialog)) {
        event.preventDefault()
        last.focus()
      } else if (!event.shiftKey && (document.activeElement === last || document.activeElement === dialog)) {
        event.preventDefault()
        first.focus()
      }
    }
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('keydown', onKeyDown)
      document.body.style.overflow = oldOverflow
      if (previousFocus?.isConnected) previousFocus.focus()
    }
  }, [])

  return <div className="login-overlay">
    <div className="login-overlay-shade" aria-hidden="true" />
    <section ref={dialogRef} tabIndex={-1}
      role="dialog" aria-modal="true" aria-labelledby="login-dialog-title"
      aria-describedby="login-dialog-description" className="login-dialog">
      <div className="login-dialog-icon" aria-hidden="true">⌗</div>
      <div className="login-dialog-heading">
        <span>SCANLOGIN</span>
        <h2 id="login-dialog-title">{title}</h2>
        <p id="login-dialog-description">{description}</p>
      </div>
      {children}
    </section>
  </div>
}
