import { useEffect, useState } from 'react';
import { ArrowRight, CheckCircle2, FilePlus2, HardHat, ShieldCheck, ClipboardList, Wrench } from 'lucide-react';
import { request } from '../api';
import { ComplaintRow, EmptyState, Metric } from '../components/ui';

const groups = [
  { key: 'all', label: 'Total reports' },
  { key: 'progress', label: 'In progress' },
  { key: 'completed', label: 'Completed' }
];

export default function OverviewPage({ user, refresh, onNavigate }) {
  const [complaints, setComplaints] = useState([]);
  const [studentCounts, setStudentCounts] = useState({ all: 0, progress: 0, completed: 0 });
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        if (user.role === 'ADMIN') {
          const result = await request('/admin/complaints');
          if (active) setComplaints(result);
        } else if (user.role === 'STUDENT') {
          const [all, progress, completed] = await Promise.all(groups.map(({ key }) =>
            request(`/student/complaints?size=1&group=${key}`)));
          if (active) {
            setStudentCounts({
              all: all.totalElements,
              progress: progress.totalElements,
              completed: completed.totalElements
            });
            const recent = await request('/student/complaints?size=100&group=all');
            if (active) setComplaints(recent.content || []);
          }
        } else {
          const result = await request(user.role === 'WORKER'
            ? '/worker/complaints'
            : '/department-admin/complaints');
          if (active) setComplaints(result.content || result || []);
        }
        if (active) setError('');
      } catch (err) {
        if (active) setError(err.message);
      }
    })();
    return () => { active = false; };
  }, [user.role, refresh]);

  const total = user.role === 'STUDENT' ? studentCounts.all : complaints.length;
  const resolved = user.role === 'STUDENT'
    ? studentCounts.completed
    : complaints.filter(c =>
      ['RESOLVED', 'CLOSED'].includes(c.status)).length;
  const pending = user.role === 'STUDENT'
    ? studentCounts.progress
    : complaints.filter(c =>
      !['RESOLVED', 'CLOSED', 'REJECTED'].includes(c.status)).length;
  const statusCounts = complaints.reduce((counts, complaint) => {
    counts[complaint.status] = (counts[complaint.status] || 0) + 1;
    return counts;
  }, {});
  const departmentCounts = complaints.reduce((counts, complaint) => {
    const department = complaint.departmentName || 'Awaiting routing';
    counts[department] = (counts[department] || 0) + 1;
    return counts;
  }, {});

  return <div className="page">
    <div className="hero-row">
      <div>
        <p className="eyebrow">TODAY AT A GLANCE</p>
        <h1>{user.role === 'STUDENT' ? 'Your campus, in motion.' : 'Make the next fix visible.'}</h1>
        <p className="muted">{user.role === 'ADMIN'
          ? 'A cross-campus pulse of reports and resolution.'
          : 'Track the work that matters without losing the human thread.'}</p>
      </div>
      {user.role === 'STUDENT' && <button className="primary"
        onClick={() => onNavigate('new')}><FilePlus2 size={17} /> Report an issue</button>}
    </div>
    {error && <div className="error">{error}</div>}
    <div className="metric-grid">
      <Metric label="Total reports" value={total} icon={ClipboardList}
        onClick={() => onNavigate('complaints', 'all')} />
      <Metric label="In progress" value={pending} icon={Wrench} accent="amber"
        onClick={() => onNavigate('complaints', 'progress')} />
      <Metric label="Completed" value={resolved} icon={CheckCircle2} accent="green"
        onClick={() => onNavigate('complaints', 'completed')} />
    </div>
    {user.role === 'ADMIN' && <section className="section-grid">
      <div className="panel">
        <div className="panel-head"><div><p className="eyebrow">STATUS MIX</p>
          <h3>Resolution health</h3></div><ShieldCheck size={20} /></div>
        {Object.entries(statusCounts).map(([status, count]) =>
          <div className="bar-row" key={status}>
            <span>{status === 'RESOLVED' ? 'Completed'
              : status === 'IN_PROGRESS' ? 'Work in progress'
                : status.replaceAll('_', ' ')}</span>
            <div className="bar"><i style={{
              width: `${total ? Math.max(5, count / total * 100) : 0}%`
            }} /></div><b>{count}</b>
          </div>)}
      </div>
      <div className="panel">
        <div className="panel-head"><div><p className="eyebrow">BY DEPARTMENT</p>
          <h3>Where attention lands</h3></div><HardHat size={20} /></div>
        {Object.entries(departmentCounts).map(([name, count]) =>
          <div className="department-row" key={name}>
            <span>{name}</span><strong>{count}<small> reports</small></strong>
          </div>)}
      </div>
    </section>}
    {user.role !== 'ADMIN' && <section className="panel">
      <div className="panel-head"><div><p className="eyebrow">RECENT ACTIVITY</p>
        <h3>Latest reports</h3></div>
        <button className="text-button" onClick={() => onNavigate('complaints', 'all')}>
          View queue <ArrowRight size={15} />
        </button>
      </div>
      {complaints.slice(0, 4).map(complaint =>
        <ComplaintRow key={complaint.id} complaint={complaint} />)}
      {!complaints.length && <EmptyState text="No reports here yet." />}
    </section>}
  </div>;
}
