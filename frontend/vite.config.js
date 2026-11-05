import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Set BACKEND_URL to point the dev proxy at a backend that is not on the default port.
const backend = process.env.BACKEND_URL || 'http://127.0.0.1:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    port: Number(process.env.PORT) || 5173,
    host: '127.0.0.1',
    proxy: {
      '/api/v1': {
        target: backend,
        changeOrigin: true,
      },
    },
  },
});
