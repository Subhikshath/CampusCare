import { useState } from 'react';
import { KeyRound } from 'lucide-react';
import { request } from '../api';

export default function ChangePasswordPage({ onDone }) {
  const [form, setForm] = useState({ currentPassword: '', newPassword: '' });
  const submit = async e => { e.preventDefault(); try { await request('/auth/change-password', { method: 'PUT', body: JSON.stringify(form) }); setForm({ currentPassword: '', newPassword: '' }); onDone('Password changed successfully'); } catch (err) { onDone(err.message); } };
  return <div className="page narrow"><div className="hero-row"><div><p className="eyebrow">ACCOUNT SECURITY</p><h1>Keep your access secure.</h1><p className="muted">Change the password for your college account.</p></div></div><form className="panel form-panel" onSubmit={submit}><div className="panel-head"><div><p className="eyebrow">PASSWORD</p><h3>Change password</h3></div><KeyRound size={20} /></div><label>Current password<input required type="password" value={form.currentPassword} onChange={e => setForm({ ...form, currentPassword: e.target.value })} /></label><label>New password<input required minLength="8" type="password" value={form.newPassword} onChange={e => setForm({ ...form, newPassword: e.target.value })} /></label><button className="primary">Update password</button></form></div>;
}
