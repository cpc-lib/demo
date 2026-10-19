import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

const keyPath = process.env.SCAN_HTTPS_KEY
const certPath = process.env.SCAN_HTTPS_CERT
if (Boolean(keyPath) !== Boolean(certPath)) {
  throw new Error('Set SCAN_HTTPS_KEY and SCAN_HTTPS_CERT together')
}
const allowedHosts = [
  '12fxxb9768168.vicp.fun',
  ...(process.env.SCAN_ALLOWED_HOSTS || '').split(',').map(host => host.trim()).filter(Boolean)
]

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { '@': resolve(__dirname, 'src') }
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/tests/setup.ts'],
    clearMocks: true,
    restoreMocks: true
  },
  server: {
    host: '0.0.0.0',
    port: 5174,
    strictPort: true,
    allowedHosts,
    https: keyPath && certPath
      ? { key: readFileSync(keyPath), cert: readFileSync(certPath) }
      : undefined,
    // PC and Mobile now run on distinct origins. Do not proxy Mobile routes.
    proxy: {
      '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true }
    }
  }
})
