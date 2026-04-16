import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App'

// root 엘리먼트 존재 확인 — 없으면 명시적 에러 메시지 출력
const rootEl = document.getElementById('root')
if (!rootEl) {
  throw new Error('root 엘리먼트를 찾을 수 없습니다. index.html에 <div id="root"> 가 있는지 확인하세요.')
}

createRoot(rootEl).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
