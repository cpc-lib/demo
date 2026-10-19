import type { ReactNode } from 'react'

type Props = {
  children: ReactNode
  mobile: boolean
  headerRight?: ReactNode
}

export function AppShell({ children, headerRight }: Props) {
  return <div className="app">
    <header className="topbar">
      <div className="brand"><span className="brand-icon">⌗</span>
        <span>ScanLogin <small>MOBILE SECURITY</small></span>
      </div>
      <nav aria-label="主要导航">
        <span className="active" aria-current="page">系统首页</span>
      </nav>
      <div className="topbar-account">
        {headerRight ?? <span className="header-guest">未登录</span>}
      </div>
    </header>
    <main className="main">{children}</main>
    <footer>Mobile · React 19 · JWT · Camera</footer>
  </div>
}
