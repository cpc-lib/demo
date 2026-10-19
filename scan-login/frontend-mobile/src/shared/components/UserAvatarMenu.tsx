import { useEffect, useRef, useState } from 'react'

type Props = {
  username: string
  mobile: boolean
  onScan?: () => void
  onLogout: () => void
}

export function UserAvatarMenu({ username, mobile, onScan, onLogout }: Props) {
  const [open, setOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    const closeOutside = (event: PointerEvent) => {
      if (!menuRef.current?.contains(event.target as Node)) setOpen(false)
    }
    const closeEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('pointerdown', closeOutside)
    document.addEventListener('keydown', closeEscape)
    return () => {
      document.removeEventListener('pointerdown', closeOutside)
      document.removeEventListener('keydown', closeEscape)
    }
  }, [open])

  const firstLetter = Array.from(username)[0]?.toUpperCase() || 'U'
  return <div className="user-menu" ref={menuRef}>
    <button className="avatar-trigger" type="button" aria-label="用户头像"
      aria-haspopup="menu" aria-expanded={open}
      onClick={() => setOpen(value => !value)}>
      <span className="avatar-circle" aria-hidden="true">{firstLetter}</span>
      <span className="avatar-username">{username}</span>
      <span aria-hidden="true" className="avatar-chevron">⌄</span>
    </button>
    {open && <div role="menu" aria-label="账户菜单" className="avatar-dropdown">
      <div className="avatar-dropdown-heading">
        <strong>{username}</strong>
        <small>{mobile ? '手机端已登录' : '电脑端已登录'}</small>
      </div>
      {mobile && <button role="menuitem" type="button"
        onClick={() => { setOpen(false); onScan?.() }}>
        <span aria-hidden="true">▣</span> 扫一扫
      </button>}
      <button role="menuitem" type="button" onClick={() => { setOpen(false); onLogout() }}>
        <span aria-hidden="true">↪</span> 退出登录
      </button>
    </div>}
  </div>
}
