import type { ReactNode } from 'react'

type Props = {
  children: ReactNode
  mobile?: boolean
  headerRight?: ReactNode
}

export function AppShell({ children, headerRight }: Props) {
  return <div className="app">
    <header className="topbar">
      <div className="brand"><span className="brand-icon">⌗</span>
        <span>ScanLogin <small>SECURITY PLATFORM</small></span>
      </div>
      <nav aria-label="主要导航">
        <a href="#/">系统首页</a>
        <a href="#/admin">管理控制台</a>
      </nav>
      <div className="topbar-account">
        {headerRight ?? <span className="header-guest">未登录</span>}
      </div>
    </header>
    <main className="main">{children}</main>
    <footer>Java 21 · Spring Boot 3 · Redis Lua · JWT · React</footer>
  </div>
}
