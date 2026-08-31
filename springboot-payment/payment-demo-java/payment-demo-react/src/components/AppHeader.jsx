import { Badge, Button, Dropdown } from 'antd'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import logo from '@/assets/img/logo.png'
import { useAuth } from '@/auth/AuthContext'

export default function AppHeader() {
  const auth = useAuth()
  const navigate = useNavigate()
  const isAdmin = auth.user?.role === 'ADMIN'

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
          {!isAdmin ? <NavLink to="/" end>课程</NavLink> : null}
          {auth.user ? <NavLink to="/orders">{isAdmin ? '全部订单' : '我的订单'}</NavLink> : null}
          {isAdmin ? <NavLink to="/refunds">退款审批</NavLink> : null}
          {isAdmin ? <NavLink to="/download">账单</NavLink> : null}
          {isAdmin ? <NavLink to="/payment-config">支付配置</NavLink> : null}
          {isAdmin ? <NavLink to="/reconciliation">对账</NavLink> : null}
        </nav>
        <div className="header-actions">
          {auth.user ? (
            <>
              {!isAdmin ? (
                <Badge count={auth.cartCount} size="small" overflowCount={99}>
                  <Button onClick={() => navigate('/cart')}>购物车</Button>
                </Badge>
              ) : null}
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
