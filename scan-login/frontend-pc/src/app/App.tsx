import { lazy, Suspense, useEffect, useState } from 'react'

const PcLoginPage = lazy(() => import('@/features/pc-login/pages/PcLoginPage'))
const AdminConsolePage = lazy(() => import('@/features/admin/pages/AdminConsolePage'))

export default function App() {
  // Hash routing keeps the admin console out of the login flow; no router dependency.
  const [isAdminRoute, setIsAdminRoute] = useState(() => window.location.hash === '#/admin')
  useEffect(() => {
    const sync = () => setIsAdminRoute(window.location.hash === '#/admin')
    window.addEventListener('hashchange', sync)
    return () => window.removeEventListener('hashchange', sync)
  }, [])
  return <Suspense fallback={<p className="page-loading" role="status">页面加载中...</p>}>
    {isAdminRoute ? <AdminConsolePage /> : <PcLoginPage />}
  </Suspense>
}
