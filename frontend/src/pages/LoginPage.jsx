import { useState } from 'react';
import { Activity, ArrowRight } from 'lucide-react';
import { demoAccounts, request } from '../api';

export default function LoginPage({ onLogin }) {
  const [email, setEmail] = useState(demoAccounts.STUDENT?.[0] ?? '');
  const [password, setPassword] = useState(demoAccounts.STUDENT?.[1] ?? '');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async (event) => {
    event.preventDefault(); setLoading(true); setError('');
    try {
      const result = await request('/auth/login', { method: 'POST', body: JSON.stringify({ collegeEmail: email, password }) });
      localStorage.setItem('campuscare_token', result.token);
      onLogin(result);
    } catch (err) { setError(err.message); } finally { setLoading(false); }
  };

  return <main className="login-shell"><section className="login-art"><div className="brand-mark">CC<span>AI</span></div><p className="kicker">CAMPUS OPERATIONS / 2026</p><h1>Turn campus issues into visible progress.</h1><p className="lede">A calm command center for reporting, routing, and resolving the everyday problems that shape student life.</p><div className="signal"><Activity size={17} /> Live resolution workspace</div></section><section className="login-panel"><div className="login-card"><p className="eyebrow">WELCOME BACK</p><h2>Sign in to CampusCare</h2><p className="muted">{Object.keys(demoAccounts).length ? 'Use a demo account below to explore each role.' : 'Sign in with your college account.'}</p><form onSubmit={submit}><label>College email<input value={email} onChange={e => setEmail(e.target.value)} type="email" /></label><label>Password<input value={password} onChange={e => setPassword(e.target.value)} type="password" /></label>{error && <div className="error">{error}</div>}<button className="primary wide" disabled={loading}>{loading ? 'Connecting...' : 'Enter workspace'} <ArrowRight size={17} /></button></form>{Object.keys(demoAccounts).length > 0 && <div className="demo-list"><span>Quick demo access</span><div>{Object.keys(demoAccounts).map(role => <button key={role} onClick={() => { setEmail(demoAccounts[role][0]); setPassword(demoAccounts[role][1]); }}>{role.replace('_', ' ')}</button>)}</div></div>}</div></section></main>;
}
