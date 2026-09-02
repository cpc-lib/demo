import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 各链路代理目标可通过环境变量覆盖（例如后端不在本机时）：
//   VITE_PROXY_NGINX=http://192.168.1.200:80 npm run dev
const targets = {
  nginx: process.env.VITE_PROXY_NGINX || 'http://localhost:80',
  gateway: process.env.VITE_PROXY_GATEWAY || 'http://localhost:8080',
  caller: process.env.VITE_PROXY_CALLER || 'http://localhost:8081',
  order: process.env.VITE_PROXY_ORDER || 'http://localhost:8082',
}

// 代理目标不可达时返回结构化诊断（Vite 默认行为是裸 500 空响应体，无法定位问题）。
// 注意：本监听器先于 Vite 自身的 error 处理注册，响应头发出后 Vite 会跳过默认 500。
function onProxyError(target) {
  return (err, _req, res) => {
    if (!res || res.headersSent || res.writableEnded) return
    res.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' })
    res.end(
      JSON.stringify({
        code: 'PROXY_TARGET_UNREACHABLE',
        message: `无法连接 ${target}（${err?.code || err?.message || 'unknown'}）。请确认该链路的后端服务已启动，或通过环境变量 VITE_PROXY_* 指定正确的代理目标。`,
      }),
    )
  }
}

function proxyTo(target, rewrite) {
  return {
    target,
    changeOrigin: true,
    rewrite,
    configure: (p) => p.on('error', onProxyError(target)),
  }
}

// 开发期代理：浏览器只访问 Vite dev server（同源，规避后端无 CORS 配置的问题），
// 由 Vite 转发到链路中的不同节点，用于对比各层的重试/拦截行为。
// 构建产物部署在 Nginx(:80) 下时，仅 /api（Nginx 全链路）可用。
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // 全链路：Nginx:80 -> Gateway:8080 -> caller:8081 -> order:8082
      '/api': proxyTo(targets.nginx),
      // 绕过 Nginx，直连 Gateway（路径重写回 /api 前缀）
      '/direct-gateway': proxyTo(targets.gateway, (p) => p.replace(/^\/direct-gateway/, '/api')),
      // 绕过 Nginx + Gateway，直连 caller-service（无 428 拦截、无 Gateway 重试）
      '/direct-caller': proxyTo(targets.caller, (p) => p.replace(/^\/direct-caller/, '')),
      // 绕过整条链路，直连 order-service 原生端点（可观察纯净的 409 错误体）
      '/direct-order': proxyTo(targets.order, (p) => p.replace(/^\/direct-order/, '')),
    },
  },
})
