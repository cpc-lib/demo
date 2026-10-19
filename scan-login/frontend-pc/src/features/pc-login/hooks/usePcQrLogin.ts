import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, errorMessage } from '@/shared/api/httpClient'
import { pcAuthApi } from '@/features/pc-login/api/pcAuthApi'
import type { PcAccount, QrStatus, QrTicket } from '@/features/pc-login/model/types'

const POLL_INTERVAL_MS = 1500

export function usePcQrLogin() {
  const [account, setAccount] = useState<PcAccount | null>(null)
  const [qr, setQr] = useState<QrTicket | null>(null)
  const [status, setStatus] = useState<QrStatus>('WAIT_SCAN')
  const [busy, setBusy] = useState(true)
  const [error, setError] = useState('')
  const mounted = useRef(false)
  const generation = useRef(0)
  const creating = useRef(false)

  /** One QR creation at a time: concurrent responses could otherwise rotate the PC binding cookie. */
  const refresh = useCallback(async () => {
    if (creating.current) return
    creating.current = true
    const sequence = ++generation.current
    setQr(null)
    setStatus('WAIT_SCAN')
    setBusy(true)
    setError('')
    try {
      const result = await pcAuthApi.createQr()
      if (mounted.current && sequence === generation.current) setQr(result)
    } catch (cause) {
      if (mounted.current && sequence === generation.current) setError(errorMessage(cause))
    } finally {
      creating.current = false
      if (mounted.current && sequence === generation.current) setBusy(false)
    }
  }, [])

  useEffect(() => {
    mounted.current = true
    let active = true
    const controller = new AbortController()
    pcAuthApi.me(controller.signal)
      .then(user => {
        if (!active) return
        setAccount(user)
        setBusy(false)
      })
      .catch(cause => {
        if (!active) return
        if (cause instanceof ApiError && (cause.status === 401 || cause.status === 403)) {
          void refresh()
        } else {
          setBusy(false)
          setError(errorMessage(cause))
        }
      })
    return () => {
      active = false
      mounted.current = false
      generation.current++
      controller.abort()
    }
  }, [refresh])

  useEffect(() => {
    if (!qr || account) return
    const sequence = generation.current
    const controller = new AbortController()
    let stopped = false
    let timeoutId: ReturnType<typeof setTimeout> | undefined

    const check = async () => {
      try {
        const result = await pcAuthApi.status(qr.ticket, controller.signal)
        if (stopped || generation.current !== sequence) return
        setStatus(result.status)
        if (result.status === 'EXPIRED') return

        if (result.status === 'CONFIRMED') {
          setBusy(true)
          await pcAuthApi.consume(qr.ticket, controller.signal)
          if (stopped || generation.current !== sequence) return
          // A successful consume is one-time: retry reading /me, never consuming again.
          const user = await pcAuthApi.me(controller.signal)
          if (!stopped && generation.current === sequence) setAccount(user)
          return
        }
        if (result.status === 'CONSUMED') {
          // Recover if the browser got its PC_ACCESS cookie but /me previously failed.
          const user = await pcAuthApi.me(controller.signal)
          if (!stopped && generation.current === sequence) setAccount(user)
          return
        }
        timeoutId = setTimeout(() => void check(), POLL_INTERVAL_MS)
      } catch (cause) {
        if (!stopped && generation.current === sequence && !(cause instanceof DOMException && cause.name === 'AbortError')) {
          setError(errorMessage(cause))
        }
      } finally {
        if (!stopped && generation.current === sequence) setBusy(false)
      }
    }
    void check()
    return () => {
      stopped = true
      controller.abort()
      if (timeoutId !== undefined) clearTimeout(timeoutId)
    }
  }, [qr, account])

  const loginWithPassword = useCallback(async (
    username: string, password: string, captchaId: string, captchaCode: string
  ): Promise<boolean> => {
    setBusy(true)
    setError('')
    try {
      await pcAuthApi.login(username, password, captchaId, captchaCode)
      const user = await pcAuthApi.me()
      if (mounted.current) setAccount(user)
      return true
    } catch (cause) {
      if (mounted.current) setError(errorMessage(cause))
      return false
    } finally {
      if (mounted.current) setBusy(false)
    }
  }, [])

  const logout = useCallback(async () => {
    setBusy(true)
    setError('')
    try {
      await pcAuthApi.logout()
      if (!mounted.current) return
      setAccount(null)
      await refresh()
    } catch (cause) {
      if (mounted.current) setError(errorMessage(cause))
    } finally {
      if (mounted.current) setBusy(false)
    }
  }, [refresh])

  return { account, qr, status, busy, error, refresh, logout, loginWithPassword }
}
