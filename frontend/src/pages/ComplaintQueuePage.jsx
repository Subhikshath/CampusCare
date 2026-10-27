import { useEffect, useState } from 'react';
import { ArrowUpRight, Building2, Search, Users } from 'lucide-react';
import { request } from '../api';
import { EmptyState, Status } from '../components/ui';
import ComplaintDetailModal from '../components/ComplaintDetailModal';
import './ComplaintQueuePage.css';

export default function ComplaintQueuePage({ user, refresh, onDone }) {
  const [items, setItems] = useState([]);
  const [workers, setWorkers] = useState([]);
  const [selected, setSelected] = useState(null);
  const [search, setSearch] = useState('');
  const [workerId, setWorkerId] = useState('');

  useEffect(() => {
    request(user.role === 'DEPARTMENT_ADMIN'
      ? '/department-admin/complaints'
      : '/worker/complaints')
      .then(setItems)
      .catch(err => onDone(err.message));

    if (user.role === 'DEPARTMENT_ADMIN') {
      request('/department-admin/workers')
        .then(setWorkers)
        .catch(err => onDone(err.message));
    }
  }, [user.role, refresh]);

  const openDetails = async item => {
    const path = user.role === 'DEPARTMENT_ADMIN'
      ? `/department-admin/complaints/${item.id}`
      : `/worker/complaints/${item.id}`;
    try {
      setSelected(await request(path));
    } catch (err) {
      onDone(err.message);
    }
  };

  const refreshSelected = async id => {
    if (!selected || selected.id !== id) return null;
    try {
      const details = await request(`/department-admin/complaints/${id}`);
      setSelected(details);
      return details;
    } catch (err) {
      const message = `The report was updated, but its details could not be refreshed: ${err.message}`;
      onDone(message);
      return null;
    }
  };

  const update = async (id, next) => {
    try {
      await request(`/department-admin/complaints/${id}/status`, {
        method: 'PUT',
        body: JSON.stringify({ status: next })
      });
      const details = await refreshSelected(id);
      onDone(next === 'RESOLVED'
        ? 'Complaint marked as completed'
        : 'Complaint status updated');
      return details;
    } catch (err) {
      onDone(err.message);
      return null;
    }
  };

  const assign = async id => {
    if (!workerId) return;
    try {
      await request(`/department-admin/complaints/${id}/worker`, {
        method: 'PUT',
        body: JSON.stringify({ workerId: Number(workerId) })
      });
      setWorkerId('');
      const details = await refreshSelected(id);
      onDone('Worker assigned');
      return details;
    } catch (err) {
      onDone(err.message);
      return null;
    }
  };

  const visible = items.filter(item => !search
    || `${item.title} ${item.description} ${item.departmentName || ''}`
      .toLowerCase().includes(search.trim().toLowerCase()));
  return <div className="page">
    <div className="hero-row queue-hero">
      <div>
        <p className="eyebrow">{user.role === 'WORKER' ? 'ASSIGNED WORK' : 'DEPARTMENT QUEUE'}</p>
        <h1>{user.role === 'WORKER' ? 'Work orders in hand.' : 'Keep the queue moving.'}</h1>
        <p className="muted">{user.role === 'WORKER'
          ? 'Your assigned campus work, with every next step in view.'
          : 'Review incoming reports, assign a worker, and keep each repair moving.'}</p>
      </div>
    </div>
    <section className="panel queue-panel">
      <div className="queue-toolbar">
        <label className="queue-search">
          <Search size={17} aria-hidden="true" />
          <input aria-label="Search reports" placeholder="Search by report or department"
            value={search} onChange={event => setSearch(event.target.value)} />
        </label>
        <span className="queue-count"><strong>{visible.length}</strong>
          {visible.length === 1 ? 'report' : 'reports'} shown</span>
      </div>
      <div className="queue-list">
        {visible.map((item, index) => <article className="queue-card" key={item.id}
          style={{ '--queue-index': index }}>
          <div className="queue-card-heading">
            <span className="queue-card-id">#{String(item.id).padStart(4, '0')}</span>
            <div className="queue-card-title">
              <h3>{item.title}</h3>
              <p>{item.description}</p>
            </div>
            <Status status={item.status} />
          </div>
          <div className="queue-card-footer">
            <div className="queue-card-meta">
              <span><Building2 size={15} />{item.departmentName || 'Awaiting department'}</span>
              {typeof item.workerAssigned === 'boolean' && <span>
                <Users size={15} />{item.workerAssigned ? 'Worker assigned' : 'Worker not assigned'}
              </span>}
            </div>
            <button className="queue-detail-button" onClick={() => openDetails(item)}>
              View details <ArrowUpRight size={15} />
            </button>
          </div>
        </article>)}
      </div>
      {!visible.length && <EmptyState text={search
        ? 'No reports match your search. Try another title or department.'
        : user.role === 'WORKER' ? 'No work has been assigned to you yet.'
          : 'The department queue is clear. New reports will appear here.'} />}
    </section>
    {selected && <ComplaintDetailModal complaint={selected}
      allowDepartmentUpdates={user.role === 'DEPARTMENT_ADMIN'}
      workers={workers}
      onAssignWorker={assign}
      onUpdateStatus={update}
      onClose={() => setSelected(null)} onDone={onDone} />}
  </div>;
}
