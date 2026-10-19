import { lazy, Suspense } from 'react'

const MobileApprovalPage = lazy(() => import('@/features/mobile-approval/pages/MobileApprovalPage'))

export default function App() {
  return <Suspense fallback={<p className="page-loading" role="status">页面加载中...</p>}>
    <MobileApprovalPage />
  </Suspense>
}
