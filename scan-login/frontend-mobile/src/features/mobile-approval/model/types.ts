export type MobileSession = { accessToken: string; refreshToken: string; username: string; userId: number }
export type MobileScan = { status: 'SCANNED' | 'CONFIRMED'; verificationCode?: string }
export type MobileOperation = 'login' | 'register' | 'scan' | 'confirm'
export type Captcha = { captchaId: string; image: string }
