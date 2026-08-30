import { Badge, Button, Dropdown } from 'antd'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import logo from '@/assets/img/logo.png'
import { useAuth } from '@/auth/AuthContext'

export default function AppHeader() {
  const auth = useAuth()
  const navigate = useNavigate()

  const logout = async () => {
    await auth.logout()
    navigate('/login')
  }

  const accountItems = [
    { key: 'account', label: <Link to="/account">账号安全</Link> },
    { key: 'logout', label: '退出登录', onClick: logout }
  ]

  return (
    <header id="header">
      <div className="container header-inner">
        <Link to="/" className="brand" title="课程支付中心">
          <img src={logo} alt="课程支付中心" />
        </Link>
        <nav className="nav" aria-label="主导航">
          <NavLink to="/" end>课程</NavLink>
          {auth.user ? <NavLink to="/orders">我的订单</NavLink> : null}
          {auth.user?.role === 'ADMIN' ? <NavLink to="/download">账单</NavLink> : null}
          {auth.user?.role === 'ADMIN' ? <NavLink to="/payment-config">支付配置</NavLink> : null}
          {auth.user?.role === 'ADMIN' ? <NavLink to="/reconciliation">对账</NavLink> : null}
        </nav>
        <div className="header-actions">
          {auth.user ? (
            <>
              <Badge count={auth.cartCount} size="small" overflowCount={99}>
                <Button onClick={() => navigate('/cart')}>购物车</Button>
              </Badge>
              <Dropdown menu={{ items: accountItems }} placement="bottomRight">
                <Button type="text">{auth.user.username}</Button>
              </Dropdown>
            </>
          ) : (
            <Button type="primary" onClick={() => navigate('/login')}>登录</Button>
          )}
        </div>
      </div>
    </header>
  )
}
