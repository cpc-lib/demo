import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发/预览模式下，/api 代理到 Gateway（默认 8080）。
// 想走完整链路（Nginx -> Gateway）可设环境变量：
//   API_PROXY_TARGET=http://localhost npm run dev
const apiProxy = {
  '/api': {
    target: process.env.API_PROXY_TARGET || 'http://localhost:8080',
    changeOrigin: true,
  },
}

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: apiProxy,
  },
  preview: {
    port: 4173,
    proxy: apiProxy,
  },
})
