import { Link, NavLink, useLocation } from 'react-router-dom'
import logo from '@/assets/img/logo.png'

export default function AppHeader() {
  const location = useLocation()

  const liClassName = (path, exact = false) => {
    if (exact) {
      return location.pathname === path ? 'current' : ''
    }
    return location.pathname.startsWith(path) ? 'current' : ''
  }

  return (
    <header id="header">
      <section className="container">
        <h1 id="logo">
          <Link to="/" title="苏三的开发日记">
            <img src={logo} width="100%" alt="苏三的开发日记" />
          </Link>
        </h1>
        <div>
          <ul className="nav">
            <li className={liClassName('/', true)}>
              <NavLink to="/" end>购买课程</NavLink>
            </li>
            <li className={liClassName('/orders')}>
              <NavLink to="/orders">我的订单</NavLink>
            </li>
            <li className={liClassName('/download')}>
              <NavLink to="/download">下载账单</NavLink>
            </li>
            <li className={liClassName('/payment-config')}>
              <NavLink to="/payment-config">支付配置</NavLink>
            </li>
            <li className={liClassName('/reconciliation')}>
              <NavLink to="/reconciliation">对账管理</NavLink>
            </li>
          </ul>
        </div>
        <div className="clear" />
      </section>
    </header>
  )
}
