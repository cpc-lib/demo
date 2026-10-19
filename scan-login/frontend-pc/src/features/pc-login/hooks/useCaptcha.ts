import { useCallback, useState } from 'react'
import { pcAuthApi } from '@/features/pc-login/api/pcAuthApi'
import type { Captcha } from '@/features/pc-login/model/types'

/** Image captchas are single-use server-side, so callers refresh after every failed login. */
export function useCaptcha() {
  const [captcha, setCaptcha] = useState<Captcha | null>(null)
  const refresh = useCallback(async () => {
    try {
      setCaptcha(await pcAuthApi.captcha())
    } catch {
      setCaptcha(null)
    }
  }, [])
  return { captcha, refresh }
}
