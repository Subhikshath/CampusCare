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

const statusLabel = status => status === 'RESOLVED' ? 'Completed'
  : status === 'IN_PROGRESS' ? 'Work in progress'
    : status === 'ON_HOLD' ? 'On hold'
      : status === 'WAITING_FOR_PARTS' ? 'Waiting for parts'
        : status === 'WAITING_FOR_ACCESS' ? 'Waiting for access'
          : status.replaceAll('_', ' ');

export default function StudentComplaintsPage({ initialGroup = 'all', refresh, onDone }) {
  const [items, setItems] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [filters, setFilters] = useState({
    search: '',
    status: '',
    departmentId: '',
    fromDate: '',
    toDate: '',
    group: initialGroup,
    workerAssigned: ''
  });
  const [selected, setSelected] = useState(null);

  const load = async () => {
    const params = new URLSearchParams({ size: 100, group: filters.group });
    Object.entries(filters).forEach(([key, value]) => {
      if (value && key !== 'group') params.set(key, value);
    });
    try {
      const result = await request(`/student/complaints?${params}`);
      setItems(result.content || []);
    } catch (err) {
      onDone(err.message);
    }
  };

  useEffect(() => {
    request('/departments')
      .then(setDepartments)
      .catch(err => onDone(err.message));
  }, []);

  useEffect(() => {
    setFilters(previous => ({ ...previous, group: initialGroup }));
  }, [initialGroup]);

  useEffect(() => {
    load();
  }, [refresh, filters.group]);

  const updateFilter = (key, value) => {
    setFilters(previous => ({ ...previous, [key]: value }));
  };

  const open = async item => {
    try {
      setSelected(await request(`/student/complaints/${item.id}`));
    } catch (err) {
      onDone(err.message);
    }
  };

  return <div className="page">
    <div className="hero-row">
      <div>
        <p className="eyebrow">STUDENT / HISTORY</p>
        <h1>Your complete report trail.</h1>
        <p className="muted">Filter reports by progress, department, worker assignment, or date. Open a report to see its complete details.</p>
      </div>
    </div>
    <section className="panel">
      <div className="filter-toolbar">
        <div className="filter-input"><Search size={16} />
          <input placeholder="Search title or description" value={filters.search}
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
          {STATUSES.map(status =>
            <option key={status} value={status}>{statusLabel(status)}</option>)}
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
        <button className="primary small" onClick={load}>
          <SlidersHorizontal size={15} /> Apply filters
        </button>
      </div>
      {items.map(item => <button className="list-button" key={item.id}
        onClick={() => open(item)}><ComplaintRow complaint={item} /></button>)}
      {!items.length && <EmptyState text="No complaints match these filters." />}
    </section>
    {selected && <ComplaintDetailModal complaint={selected} allowFollowUp
      onClose={() => setSelected(null)}
      onDone={message => { onDone(message); load(); }} />}
  </div>;
}
