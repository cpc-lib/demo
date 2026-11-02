import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
// Dev proxy: browser -> vite(5173) -> Spring Boot(8080)
// Avoids CORS without touching backend CORS config.
export default defineConfig({
    plugins: [react()],
    server: {
        port: 5173,
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
        },
    },
});
