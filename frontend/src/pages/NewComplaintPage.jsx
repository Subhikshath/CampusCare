import { useState } from 'react';
import { ArrowRight, CheckCircle2, RotateCcw, Send, Sparkles } from 'lucide-react';
import { request } from '../api';
import { Status } from '../components/ui';
import '../report.css';

export default function NewComplaintPage({ onDone, onNavigate }) {
  const emptyForm = { title: '', description: '', additionalInformation: '' };
  const [form, setForm] = useState(emptyForm);
  const [reply, setReply] = useState('');
  const [loading, setLoading] = useState(false);
  const [complaint, setComplaint] = useState(null);
  const [messages, setMessages] = useState([]);
  const [error, setError] = useState('');

  const submitComplaint = async event => {
    event.preventDefault();
    setLoading(true);
    setError('');
    try {
      const data = await request('/student/complaints', {
        method: 'POST',
        body: JSON.stringify(form)
      });
      setComplaint({ ...data, additionalInformation: form.additionalInformation.trim() });
      setMessages([
        { role: 'student', text: `${form.title}\n${form.description}` },
        ...(form.additionalInformation.trim()
          ? [{ role: 'student', text: form.additionalInformation.trim() }]
          : []),
        {
          role: 'ai',
          text: data.followUpQuestion
            || `Thanks — I have enough detail to route this report to ${data.departmentName || 'the right team'}.`
        }
      ]);
      onDone(data.followUpQuestion
        ? 'Report saved. Please answer the AI follow-up to continue.'
        : 'Complaint submitted and routed');
    } catch (err) {
      setError(err.message);
      onDone(err.message);
    } finally {
      setLoading(false);
    }
  };

  const submitReply = async event => {
    event.preventDefault();
    const answer = reply.trim();
    if (!answer || !complaint) return;
    setLoading(true);
    setError('');
    try {
      const data = await request(`/student/complaints/${complaint.id}/follow-up`, {
        method: 'POST',
        body: JSON.stringify({ additionalInformation: answer })
      });
      setComplaint(data);
      setMessages(previous => [
        ...previous,
        { role: 'student', text: answer },
        {
          role: 'ai',
          text: data.followUpQuestion
            || `That gives me enough context. Your report has been routed to ${data.departmentName || 'the relevant team'}.`
        }
      ]);
      setReply('');
      onDone(data.followUpQuestion
        ? 'Thanks — the AI has one more question.'
        : 'Your report has been routed to the relevant team.');
    } catch (err) {
      setError(err.message);
      onDone(err.message);
    } finally {
      setLoading(false);
    }
  };

  const startAnother = () => {
    setComplaint(null);
    setMessages([]);
    setReply('');
    setError('');
    setForm(emptyForm);
  };

  return <div className="page narrow new-report-page">
    <div className="hero-row">
      <div>
        <p className="eyebrow">CAMPUS CARE / NEW REPORT</p>
        <h1>Let’s get this sorted.</h1>
        <p className="muted">Report an issue anywhere on campus. Our AI will ask for useful details and route it to the right department.</p>
      </div>
      <div className="report-orbit" aria-hidden="true"><Sparkles size={25} /></div>
    </div>

    {!complaint ? <form className="panel form-panel report-form" onSubmit={submitComplaint}>
      <div className="form-intro"><span className="form-step">01</span><div><strong>Tell us what’s going on</strong><small>Clear details help us resolve it faster.</small></div></div>
      <label>Short title<input required maxLength="200" value={form.title} onChange={event => setForm({ ...form, title: event.target.value })} placeholder="e.g. Water leak outside the library" /></label>
      <label>What happened?<textarea required rows="5" value={form.description} onChange={event => setForm({ ...form, description: event.target.value })} placeholder="Tell us what you noticed and where it is happening." /></label>
      <label>Anything else? <span className="optional">Optional</span><textarea rows="3" value={form.additionalInformation} onChange={event => setForm({ ...form, additionalInformation: event.target.value })} placeholder="When did it start? Is anyone affected? Add any helpful context." /></label>
      {error && <p className="error" role="alert">{error}</p>}
      <div className="form-footer"><span><Sparkles size={15} /> AI-guided report intake</span><button className="primary" disabled={loading}>{loading ? 'Analyzing your report...' : 'Submit'} <ArrowRight size={17} /></button></div>
    </form> : <section className="panel ai-intake-card" aria-label="AI report follow-up">
      <div className="intake-heading">
        <div className="intake-icon"><Sparkles size={19} /></div>
        <div><p className="eyebrow">AI-GUIDED INTAKE</p><h2>{complaint.followUpQuestion ? 'A couple more details' : 'Your report is ready'}</h2></div>
        <Status status={complaint.status} />
      </div>
      <div className="intake-summary">
        <span>REPORT #{complaint.id}</span>
        <strong>{complaint.title}</strong>
        <p>{complaint.description}</p>
        {complaint.additionalInformation && <>
          <strong>Additional information</strong>
          <p>{complaint.additionalInformation}</p>
        </>}
        <div className="detail-grid">
          <span>Current status<strong><Status status={complaint.status} /></strong></span>
          <span>Department<strong>{complaint.departmentName || 'Awaiting more information'}</strong></span>
          <span>Assigned worker<strong>{complaint.assignedWorkerId || 'Not assigned yet'}</strong></span>
          <span>Submitted<strong>{complaint.createdAt ? new Date(complaint.createdAt).toLocaleString() : 'Just now'}</strong></span>
        </div>
      </div>
      <div className="chat-thread" aria-live="polite">
        {messages.map((message, index) => <div className={`chat-message ${message.role}`} key={`${index}-${message.role}`}>
          <span className="chat-avatar">{message.role === 'ai' ? <Sparkles size={15} /> : 'Y'}</span>
          <div className="chat-bubble"><small>{message.role === 'ai' ? 'CAMPUS CARE AI' : 'YOU'}</small><p>{message.text}</p></div>
        </div>)}
        {loading && <div className="chat-thinking"><span /><span /><span /> AI is reviewing your details</div>}
      </div>
      {complaint.followUpQuestion ? <form className="chat-reply" onSubmit={submitReply}>
        <label htmlFor="ai-reply">Your reply</label>
        <div className="reply-controls"><textarea id="ai-reply" required rows="2" value={reply} onChange={event => setReply(event.target.value)} placeholder="Type your answer here..." disabled={loading} /><button className="primary" disabled={loading || !reply.trim()}>{loading ? 'Checking...' : 'Send reply'} <Send size={16} /></button></div>
        {error && <p className="error" role="alert">{error}</p>}
        <small>The AI may ask another question if one more detail is needed.</small>
      </form> : <div className="intake-complete">
        <CheckCircle2 size={21} /><div>        <strong>All set — your report is with {complaint.departmentName || 'the right team'}.</strong><span>Our team will follow up as the issue is reviewed.</span></div>
        <button className="secondary" onClick={() => onNavigate?.('complaints')}>View all report details</button>
        <button className="secondary" onClick={startAnother}><RotateCcw size={15} /> Submit another issue</button>
      </div>}
    </section>}
  </div>;
}
