import { Navigate, useLocation } from 'react-router-dom'
import { Skeleton } from 'antd'

import { useAuth } from '@/auth/AuthContext'

export default function ProtectedRoute({ children, role }) {
  const auth = useAuth()
  const location = useLocation()

  if (!auth.bootstrapped) {
    return <main className="container page-shell"><Skeleton active paragraph={{ rows: 5 }} /></main>
  }
  if (!auth.user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  if (role && auth.user.role !== role) {
    return <Navigate to="/" replace />
  }
  return children
}
