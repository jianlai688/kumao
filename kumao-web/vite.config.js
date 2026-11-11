import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'

const proxy = {
  '/api': {
    target: process.env.VITE_API_TARGET || 'http://127.0.0.1:8080',
    changeOrigin: true
  },
  '/ws': {
    target: process.env.VITE_WS_TARGET || 'ws://127.0.0.1:8080',
    ws: true
  },
  '/static': {
    target: process.env.VITE_API_TARGET || 'http://127.0.0.1:8080',
    changeOrigin: true
  }
}

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy,
    watch: {
      // Atomic-save tools and the harness create short-lived tmpdirs inside src;
      // watching them can throw EBUSY on Windows and crash the Vite watcher.
      ignored: ['**/*.tmp', '**/.*.tmpdir/**', '**/*.tmpdir/**']
    }
  },
  preview: {
    port: 4173,
    host: '127.0.0.1',
    proxy
  }
})
