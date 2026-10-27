import { useCallback, useEffect, useRef, useState } from 'react';
import { ArrowRight, Sparkles } from 'lucide-react';
import { request } from '../api';
import '../report.css';

export default function ChatPage() {
  const [message, setMessage] = useState('');
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const threadRef = useRef(null);

  const loadHistory = useCallback(async (preserveError = false) => {
    try {
      setMessages(await request('/chatbot/messages'));
      if (!preserveError) setError('');
      return null;
    } catch (err) {
      if (!preserveError) setError(err.message);
      return err.message;
    }
  }, []);

  useEffect(() => {
    loadHistory();
  }, [loadHistory]);

  useEffect(() => {
    threadRef.current?.scrollTo({ top: threadRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages, loading]);

  const send = async event => {
    event.preventDefault();
    const question = message.trim();
    if (!question || loading) return;

    setLoading(true);
    setError('');
    try {
      const result = await request('/chatbot/messages', {
        method: 'POST',
        body: JSON.stringify({ message: question })
      });
      setMessages(previous => [
        ...previous,
        { id: `user-${result.id}`, sender: 'USER', message: question, createdAt: null },
        { id: result.id, sender: 'ASSISTANT', message: result.reply, createdAt: result.createdAt }
      ]);
      setMessage('');
    } catch (err) {
      const historyError = await loadHistory(true);
      setError(historyError
        ? `${err.message} Chat history could not be refreshed: ${historyError}`
        : err.message);
    } finally {
      setLoading(false);
    }
  };

  return <div className="page narrow portal-chat-page">
    <div className="hero-row">
      <div>
        <p className="eyebrow">CAMPUSCARE ASSISTANT</p>
        <h1>Ask a practical question.</h1>
        <p className="muted">Get answers about using CampusCare. The assistant uses portal guidance only and cannot access private complaint records.</p>
      </div>
    </div>
    <section className="panel portal-chat-panel" aria-label="CampusCare portal assistant">
      <div className="portal-chat-thread chat-thread" ref={threadRef} aria-live="polite">
        {messages.length === 0 && !error && <div className="chat-message assistant">
          <span className="chat-avatar"><Sparkles size={15} /></span>
          <div className="chat-bubble"><small>CAMPUSCARE ASSISTANT</small><p>Hi. Ask me how to submit or track a campus issue, answer an intake question, or use another part of this portal.</p></div>
        </div>}
        {messages.map(item => <div className={`chat-message ${item.sender === 'USER' ? 'student' : 'assistant'}`} key={item.id}>
          <span className="chat-avatar">{item.sender === 'USER' ? 'Y' : <Sparkles size={15} />}</span>
          <div className="chat-bubble">
            <small>{item.sender === 'USER' ? 'YOU' : 'CAMPUSCARE ASSISTANT'}</small>
            <p>{item.message}</p>
          </div>
        </div>)}
        {loading && <div className="chat-thinking"><span /><span /><span /> Checking CampusCare guidance</div>}
      </div>
      {error && <p className="error" role="alert">{error}</p>}
      <form className="portal-chat-form" onSubmit={send}>
        <textarea required maxLength={4000} rows="3" value={message} onChange={event => setMessage(event.target.value)} placeholder="How can I submit an issue in this portal?" aria-label="Your question" />
        <button className="primary" disabled={loading || !message.trim()}>{loading ? 'Thinking...' : 'Send message'} <ArrowRight size={17} /></button>
      </form>
    </section>
  </div>;
}
