import { HashRouter, Route, Routes } from 'react-router-dom'

import AppHeader from './components/AppHeader.jsx'
import AppFooter from './components/AppFooter.jsx'
import Home from './pages/Home.jsx'
import Orders from './pages/Orders.jsx'
import Download from './pages/Download.jsx'
import Success from './pages/Success.jsx'
import PaymentConfig from './pages/PaymentConfig.jsx'
import Reconciliation from './pages/Reconciliation.jsx'
import Login from './pages/Login.jsx'
import Cart from './pages/Cart.jsx'
import Account from './pages/Account.jsx'
import Refunds from './pages/Refunds.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import { AuthProvider } from './auth/AuthContext.jsx'

export default function App() {
  return (
    <HashRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <AuthProvider>
        <div id="app">
          <AppHeader />
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/cart" element={<ProtectedRoute role="USER"><Cart /></ProtectedRoute>} />
            <Route path="/orders" element={<ProtectedRoute><Orders /></ProtectedRoute>} />
            <Route path="/account" element={<ProtectedRoute><Account /></ProtectedRoute>} />
            <Route path="/download" element={<ProtectedRoute role="ADMIN"><Download /></ProtectedRoute>} />
            <Route path="/payment-config" element={<ProtectedRoute role="ADMIN"><PaymentConfig /></ProtectedRoute>} />
            <Route path="/reconciliation" element={<ProtectedRoute role="ADMIN"><Reconciliation /></ProtectedRoute>} />
            <Route path="/refunds" element={<ProtectedRoute role="ADMIN"><Refunds /></ProtectedRoute>} />
            <Route path="/success" element={<ProtectedRoute role="USER"><Success /></ProtectedRoute>} />
          </Routes>
          <AppFooter />
        </div>
      </AuthProvider>
    </HashRouter>
  )
}
