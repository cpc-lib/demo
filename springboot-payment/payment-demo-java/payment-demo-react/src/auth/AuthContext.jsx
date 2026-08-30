import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'

import authApi from '@/api/auth'
import cartApi from '@/api/cart'
import {
  clearSession,
  getSession,
  refreshSession,
  setSession,
  subscribeSession
} from './session'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [session, setSessionState] = useState(getSession())
  const [cartCount, setCartCount] = useState(0)

  useEffect(() => subscribeSession(setSessionState), [])

  useEffect(() => {
    if (!getSession().bootstrapped) {
      refreshSession().catch(() => {})
    }
  }, [])

  const refreshCartCount = useCallback(() => {
    if (!getSession().user) {
      setCartCount(0)
      return Promise.resolve()
    }
    return cartApi.get().then((response) => {
      setCartCount(response?.data?.totalQuantity || 0)
    }).catch(() => {
      setCartCount(0)
    })
  }, [])

  useEffect(() => {
    refreshCartCount()
  }, [session.user?.userId, refreshCartCount])

  const login = useCallback(async (credentials) => {
    const response = await authApi.login(credentials)
    setSession(response.data)
    return response.data
  }, [])

  const register = useCallback(async (data) => {
    const response = await authApi.register(data)
    setSession(response.data)
    return response.data
  }, [])

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      clearSession()
      setCartCount(0)
    }
  }, [])

  const value = useMemo(() => ({
    ...session,
    cartCount,
    login,
    register,
    logout,
    refreshCartCount
  }), [session, cartCount, login, register, logout, refreshCartCount])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider')
  }
  return context
}
