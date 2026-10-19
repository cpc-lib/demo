import net from 'node:net'
import https from 'node:https'
import { createServer } from 'vite'

const port = 5174
const address = '0.0.0.0'
const homePath = '/'
const title = 'ScanLogin PC'
const scheme = process.env.SCAN_HTTPS_KEY && process.env.SCAN_HTTPS_CERT ? 'https' : 'http'

async function portAvailable() {
  return new Promise((resolve, reject) => {
    const socket = net.createServer()
    socket.once('error', error => {
      if (error.code === 'EADDRINUSE') resolve(false)
      else reject(error)
    })
    socket.listen(port, address, () => socket.close(() => resolve(true)))
  })
}

async function currentApp() {
  for (const candidate of ['http', 'https']) {
    try {
      const body = candidate === 'http'
        ? await (await fetch(`http://127.0.0.1:${port}${homePath}`,
            { signal: AbortSignal.timeout(2200) })).text()
        : await new Promise((resolve, reject) => {
          const req = https.get({
            hostname: '127.0.0.1', port, path: homePath,
            timeout: 2200, rejectUnauthorized: false
          }, response => {
            let html = ''
            response.setEncoding('utf8')
            response.on('data', value => { html += value })
            response.on('end', () => resolve(html))
          })
          req.once('timeout', () => req.destroy(new Error('timeout')))
          req.once('error', reject)
        })
      if (body.includes(title) && body.includes('main.tsx')) return candidate
    } catch {
      // The listener may be using the other protocol.
    }
  }
  return null
}

async function main() {
  if (!(await portAvailable())) {
    const existing = await currentApp()
    if (existing) {
      console.log(`${title} 已在运行：${existing}://localhost:${port}${homePath}`)
      return
    }
    throw new Error(`端口 ${port} 被其他程序占用，请先检查端口归属`)
  }

  const server = await createServer({ server: { host: address, port, strictPort: true } })
  await server.listen()
  server.printUrls()
  console.log(`${title}: ${scheme}://localhost:${port}${homePath}`)
  const close = async () => {
    await server.close()
    process.exit(0)
  }
  process.once('SIGINT', () => void close())
  process.once('SIGTERM', () => void close())
}
main().catch(error => {
  console.error(error)
  process.exitCode = 1
})
