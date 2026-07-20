import { HashRouter, Route, Routes } from 'react-router-dom'

import AppHeader from './components/AppHeader.jsx'
import AppFooter from './components/AppFooter.jsx'
import Home from './pages/Home.jsx'
import Orders from './pages/Orders.jsx'
import Download from './pages/Download.jsx'
import Success from './pages/Success.jsx'
import PaymentConfig from './pages/PaymentConfig.jsx'
import Reconciliation from './pages/Reconciliation.jsx'

export default function App() {
  return (
    <HashRouter>
      <div id="app">
        <AppHeader />
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/orders" element={<Orders />} />
          <Route path="/download" element={<Download />} />
          <Route path="/payment-config" element={<PaymentConfig />} />
          <Route path="/reconciliation" element={<Reconciliation />} />
          <Route path="/success" element={<Success />} />
        </Routes>
        <AppFooter />
      </div>
    </HashRouter>
  )
}
