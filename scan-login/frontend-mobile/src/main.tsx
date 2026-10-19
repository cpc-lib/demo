import { createRoot } from 'react-dom/client'
import App from '@/app/App'
import '@/shared/styles/base.css'
import '@/shared/styles/shell.css'
import '@/shared/styles/auth.css'
import '@/shared/styles/dashboard.css'

const container = document.getElementById('root')
if (!container) throw new Error('Missing application root')
createRoot(container).render(<App />)
