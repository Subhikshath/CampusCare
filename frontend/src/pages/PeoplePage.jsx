import { useEffect, useState } from 'react';
import { Users } from 'lucide-react';
import { request } from '../api';
import { EmptyState } from '../components/ui';

export default function PeoplePage({ refresh, onDone }) {
  const [workers, setWorkers] = useState([]);
  useEffect(() => { request('/admin/workers').then(setWorkers).catch(err => onDone(err.message)); }, [refresh]);
  return <div className="page"><div className="hero-row"><div><p className="eyebrow">PEOPLE & ACCESS</p><h1>Keep the right people in the loop.</h1></div></div><section className="panel"><div className="panel-head"><div><p className="eyebrow">WORKERS</p><h3>Field team</h3></div><Users size={20} /></div>{workers.map(worker => <div className="person-row" key={worker.userId}><div className="avatar">{worker.name.slice(0, 1)}</div><div><strong>{worker.name}</strong><span>{worker.collegeEmail}</span></div><span className={`status ${worker.enabled ? 'resolved' : 'rejected'}`}>{worker.enabled ? 'ENABLED' : 'DISABLED'}</span></div>)}{!workers.length && <EmptyState text="No workers have been added." />}</section></div>;
}
