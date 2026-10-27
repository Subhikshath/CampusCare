import { ClipboardList } from 'lucide-react';

export function Metric({ label, value, icon: Icon, accent = '', onClick }) {
  const content = <><div className="metric-icon"><Icon size={18} /></div><span>{label}</span><strong>{value}</strong></>;
  return onClick
    ? <button type="button" className={`metric metric-button ${accent}`} onClick={onClick}>{content}</button>
    : <div className={`metric ${accent}`}>{content}</div>;
}

export function ComplaintRow({ complaint }) {
  return <div className="complaint-row"><div className="complaint-id">#{String(complaint.id).padStart(4, '0')}</div><div className="complaint-main"><strong>{complaint.title}</strong><span>{complaint.description}</span></div><Status status={complaint.status} /><div className="complaint-meta"><span>{complaint.departmentName || (complaint.departmentId ? `Dept. ${complaint.departmentId}` : 'Awaiting routing')}</span>{typeof complaint.workerAssigned === 'boolean' && <span>{complaint.workerAssigned ? 'Worker assigned' : 'Worker not assigned'}</span>}</div></div>;
}

export function Status({ status }) {
  return <span className={`status ${status?.toLowerCase()}`}>{formatStatus(status)}</span>;
}

export function formatStatus(status) {
  if (status === 'RESOLVED') return 'Completed';
  if (status === 'IN_PROGRESS') return 'Work in progress';
  if (status === 'ON_HOLD') return 'On hold';
  if (status === 'WAITING_FOR_PARTS') return 'Waiting for parts';
  if (status === 'WAITING_FOR_ACCESS') return 'Waiting for access';
  return status?.replaceAll('_', ' ');
}

export function EmptyState({ text }) {
  return <div className="empty"><ClipboardList size={23} /><span>{text}</span></div>;
}
