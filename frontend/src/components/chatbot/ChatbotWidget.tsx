import { useState, useEffect, useRef } from 'react'
import { fetchChatbotStatus, askChatbot } from '../../api/ai'

interface Message {
  role: 'user' | 'assistant'
  content: string
}

// 팬 챗봇 위젯 — 우하단 고정 버튼으로 열고 닫기
// AI_ENABLED=0이면 위젯 자체를 렌더링하지 않음
export function ChatbotWidget() {
  const [available, setAvailable] = useState(false)
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  // 챗봇 활성화 여부 확인
  useEffect(() => {
    fetchChatbotStatus().then(setAvailable)
  }, [])

  // 메시지 추가 시 스크롤 하단으로
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  // AI 비활성화 시 위젯 미표시
  if (!available) return null

  const handleSend = async () => {
    const question = input.trim()
    if (!question || loading) return

    const userMessage: Message = { role: 'user', content: question }
    setMessages(prev => [...prev, userMessage])
    setInput('')
    setLoading(true)

    try {
      // 최근 2턴(4개 메시지)만 전달
      const history = messages.slice(-4).map(m => ({ role: m.role, content: m.content }))
      const answer = await askChatbot(question, history)
      setMessages(prev => [...prev, { role: 'assistant', content: answer }])
    } catch {
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: '잠시 후 다시 시도해주세요.'
      }])
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      {/* 챗봇 열기 버튼 */}
      <button
        onClick={() => setOpen(prev => !prev)}
        className="fixed bottom-6 right-6 z-50 w-12 h-12 rounded-full bg-primary text-primary-foreground shadow-lg flex items-center justify-center text-xl hover:opacity-90 transition-opacity"
        aria-label="챗봇 열기"
      >
        {open ? '✕' : '💬'}
      </button>

      {/* 챗봇 패널 */}
      {open && (
        <div className="fixed bottom-20 right-6 z-50 w-80 h-96 bg-background border border-border rounded-xl shadow-xl flex flex-col overflow-hidden">
          {/* 헤더 */}
          <div className="px-4 py-3 bg-primary text-primary-foreground text-sm font-medium">
            E-sports 도우미
          </div>

          {/* 메시지 목록 */}
          <div className="flex-1 overflow-y-auto p-3 space-y-2">
            {messages.length === 0 && (
              <p className="text-xs text-muted-foreground text-center mt-4">
                경기 일정, 결과, 팀 정보를 물어보세요!
              </p>
            )}
            {messages.map((msg, i) => (
              <div
                key={i}
                className={`text-xs px-3 py-2 rounded-lg max-w-[85%] ${
                  msg.role === 'user'
                    ? 'ml-auto bg-primary text-primary-foreground'
                    : 'mr-auto bg-muted text-foreground'
                }`}
              >
                {msg.content}
              </div>
            ))}
            {loading && (
              <div className="mr-auto text-xs px-3 py-2 rounded-lg bg-muted text-muted-foreground">
                답변 생성 중...
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* 입력창 */}
          <div className="flex gap-2 p-2 border-t border-border">
            <input
              value={input}
              onChange={e => setInput(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleSend()}
              placeholder="질문을 입력하세요..."
              disabled={loading}
              className="flex-1 text-xs px-3 py-2 rounded-md border border-border bg-background focus:outline-none focus:ring-1 focus:ring-primary disabled:opacity-50"
              maxLength={500}
            />
            <button
              onClick={handleSend}
              disabled={loading || !input.trim()}
              className="text-xs px-3 py-2 rounded-md bg-primary text-primary-foreground disabled:opacity-50 hover:opacity-90 transition-opacity"
            >
              전송
            </button>
          </div>
        </div>
      )}
    </>
  )
}
