import { useEffect, useState } from 'react';
import { CalendarDays, Search, SlidersHorizontal } from 'lucide-react';
import { request } from '../api';
import { ComplaintRow, EmptyState } from '../components/ui';
import ComplaintDetailModal from '../components/ComplaintDetailModal';

const STATUSES = [
  'AWAITING_INFORMATION', 'DEPARTMENT_ASSIGNED', 'PENDING_DEPARTMENT_REVIEW',
  'WORKER_ASSIGNED', 'IN_PROGRESS', 'ON_HOLD', 'WAITING_FOR_PARTS',
  'WAITING_FOR_ACCESS', 'RESOLVED', 'CLOSED', 'REJECTED'
];

export default function AdminComplaintsPage({ initialGroup = 'all', refresh, onDone }) {
  const [items, setItems] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [selected, setSelected] = useState(null);
  const [filters, setFilters] = useState({
    search: '',
    status: '',
    departmentId: '',
    fromDate: '',
    toDate: '',
    group: initialGroup,
    workerAssigned: ''
  });

  useEffect(() => {
    request('/admin/complaints').then(setItems).catch(err => onDone(err.message));
    request('/departments').then(setDepartments).catch(err => onDone(err.message));
  }, [refresh]);

  useEffect(() => {
    setFilters(previous => ({ ...previous, group: initialGroup }));
  }, [initialGroup]);

  const updateFilter = (key, value) => {
    setFilters(previous => ({ ...previous, [key]: value }));
  };

  const open = async item => {
    try {
      setSelected(await request(`/admin/complaints/${item.id}`));
    } catch (err) {
      onDone(err.message);
    }
  };

  const updateStatus = async (id, status) => {
    const updated = await request(`/admin/complaints/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status })
    });
    onDone(status === 'RESOLVED' ? 'Report marked as completed' : 'Report status updated');
    const details = await request(`/admin/complaints/${id}`);
    setSelected(details);
    return details || updated;
  };

  const visible = items.filter(item => {
    const searchText = `${item.title} ${item.description}`.toLowerCase();
    const created = item.createdAt ? new Date(item.createdAt) : null;
    const from = filters.fromDate ? new Date(`${filters.fromDate}T00:00:00`) : null;
    const to = filters.toDate ? new Date(`${filters.toDate}T23:59:59.999`) : null;
    const groupMatches = filters.group === 'all'
      || (filters.group === 'progress'
        ? !['RESOLVED', 'CLOSED', 'REJECTED'].includes(item.status)
        : ['RESOLVED', 'CLOSED'].includes(item.status));
    return (!filters.search || searchText.includes(filters.search.toLowerCase()))
      && (!filters.status || item.status === filters.status)
      && (!filters.departmentId || String(item.departmentId) === filters.departmentId)
      && (!filters.workerAssigned || String(item.workerAssigned) === filters.workerAssigned)
      && (!from || (created && created >= from))
      && (!to || (created && created <= to))
      && groupMatches;
  });

  return <div className="page">
    <div className="hero-row"><div>
      <p className="eyebrow">ADMIN / ALL COMPLAINTS</p>
      <h1>The whole campus in one view.</h1>
      <p className="muted">Monitor reports, inspect complete details, and update the status as work progresses.</p>
    </div></div>
    <section className="panel">
      <div className="filter-toolbar">
        <div className="filter-input"><Search size={16} />
          <input placeholder="Search reports" value={filters.search}
            onChange={event => updateFilter('search', event.target.value)} />
        </div>
        <select aria-label="Report group" value={filters.group}
          onChange={event => updateFilter('group', event.target.value)}>
          <option value="all">All reports</option>
          <option value="progress">In progress</option>
          <option value="completed">Completed</option>
        </select>
        <select aria-label="Status" value={filters.status}
          onChange={event => updateFilter('status', event.target.value)}>
          <option value="">All statuses</option>
          {STATUSES.map(status => <option key={status} value={status}>
            {status === 'RESOLVED' ? 'Completed'
              : status === 'IN_PROGRESS' ? 'Work in progress'
                : status.replaceAll('_', ' ')}
          </option>)}
        </select>
        <select aria-label="Department" value={filters.departmentId}
          onChange={event => updateFilter('departmentId', event.target.value)}>
          <option value="">All departments</option>
          {departments.map(department =>
            <option key={department.id} value={department.id}>{department.name}</option>)}
        </select>
        <select aria-label="Worker assignment" value={filters.workerAssigned}
          onChange={event => updateFilter('workerAssigned', event.target.value)}>
          <option value="">Any worker assignment</option>
          <option value="true">Worker assigned</option>
          <option value="false">Worker not assigned</option>
        </select>
        <label className="date-input"><CalendarDays size={15} />
          <input aria-label="From date" type="date" value={filters.fromDate}
            onChange={event => updateFilter('fromDate', event.target.value)} />
        </label>
        <label className="date-input"><CalendarDays size={15} />
          <input aria-label="To date" type="date" value={filters.toDate}
            onChange={event => updateFilter('toDate', event.target.value)} />
        </label>
        <span><SlidersHorizontal size={14} /> {visible.length} reports</span>
      </div>
      {visible.map(item => <button className="list-button" key={item.id}
        onClick={() => open(item)}><ComplaintRow complaint={item} /></button>)}
      {!visible.length && <EmptyState text="No reports match these filters." />}
    </section>
    {selected && <ComplaintDetailModal complaint={selected}
      allowAdminUpdates onUpdateStatus={updateStatus}
      onClose={() => setSelected(null)} onDone={onDone} />}
  </div>;
}
