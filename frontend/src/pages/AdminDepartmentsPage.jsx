import { useEffect, useState } from 'react';
import { Building2, Plus, Power, Save } from 'lucide-react';
import { request } from '../api';
import { EmptyState } from '../components/ui';

const blank = { name: '', code: '' };

export default function AdminDepartmentsPage({ refresh, onDone }) {
  const [departments, setDepartments] = useState([]); const [form, setForm] = useState(blank); const [editing, setEditing] = useState(null);
  const load = () => request('/admin/departments').then(setDepartments).catch(err => onDone(err.message));
  useEffect(load, [refresh]);
  const save = async e => { e.preventDefault(); try { await request(editing ? `/admin/departments/${editing}` : '/admin/departments', { method: editing ? 'PUT' : 'POST', body: JSON.stringify(form) }); setForm(blank); setEditing(null); onDone(editing ? 'Department updated' : 'Department created'); } catch (err) { onDone(err.message); } };
  const edit = item => { setEditing(item.id); setForm({ name: item.name, code: item.code }); };
  const disable = async id => { try { await request(`/admin/departments/${id}`, { method: 'DELETE' }); onDone('Department disabled'); } catch (err) { onDone(err.message); } };
  return <div className="page"><div className="hero-row"><div><p className="eyebrow">ADMIN / DEPARTMENTS</p><h1>Shape the campus routing map.</h1><p className="muted">Create, rename, and safely disable departments without removing history.</p></div></div><div className="section-grid"><form className="panel form-panel" onSubmit={save}><div className="panel-head"><div><p className="eyebrow">{editing ? 'EDIT DEPARTMENT' : 'NEW DEPARTMENT'}</p><h3>{editing ? 'Update routing details' : 'Add a department'}</h3></div><Building2 size={20} /></div><label>Name<input required value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} placeholder="Facilities" /></label><label>Code<input required value={form.code} onChange={e => setForm({ ...form, code: e.target.value.toUpperCase() })} placeholder="FACILITIES" /></label><div className="form-actions"><button className="primary"><Save size={16} /> {editing ? 'Save changes' : 'Create department'}</button>{editing && <button type="button" className="secondary" onClick={() => { setEditing(null); setForm(blank); }}>Cancel</button>}</div></form><section className="panel"><div className="panel-head"><div><p className="eyebrow">DIRECTORY</p><h3>{departments.length} departments</h3></div><Plus size={20} /></div>{departments.map(item => <div className="admin-row" key={item.id}><div><strong>{item.name}</strong><span>{item.code}</span></div><span className={`status ${item.enabled ? 'resolved' : 'rejected'}`}>{item.enabled ? 'ENABLED' : 'DISABLED'}</span><button className="text-button" onClick={() => edit(item)}>Edit</button>{item.enabled && <button className="icon-button" title="Disable" onClick={() => disable(item.id)}><Power size={15} /></button>}</div>)}{!departments.length && <EmptyState text="No departments configured." />}</section></div></div>;
}
