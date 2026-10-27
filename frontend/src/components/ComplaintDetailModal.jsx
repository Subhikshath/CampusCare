import { useState } from 'react';
import { Send } from 'lucide-react';
import { request } from '../api';
import { formatStatus, Status } from './ui';
import '../report.css';

export default function ComplaintDetailModal({
  complaint,
  onClose,
  onDone,
  allowFollowUp = false,
  allowDepartmentUpdates = false,
  allowAdminUpdates = false,
  workers = [],
  onAssignWorker,
  onUpdateStatus
}) {
  const [details, setDetails] = useState(complaint);
  const [followUp, setFollowUp] = useState('');
  const [workerId, setWorkerId] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const assignWorker = async event => {
    event.preventDefault();
    if (!workerId || busy) return;
    setBusy(true);
    setError('');
    try {
      const updated = await onAssignWorker(details.id, Number(workerId));
      if (updated) {
        setDetails(updated);
        setWorkerId('');
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const updateStatus = async status => {
    if (busy) return;
    setBusy(true);
    setError('');
    try {
      const updated = await onUpdateStatus(details.id, status);
      if (updated) setDetails(updated);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const submitFollowUp = async event => {
    event.preventDefault();
    const answer = followUp.trim();
    if (!answer || busy) return;

    setBusy(true);
    setError('');
    try {
      const updated = await request(`/student/complaints/${details.id}/follow-up`, {
        method: 'POST',
        body: JSON.stringify({ additionalInformation: answer })
      });
      setFollowUp('');
      setDetails(previous => ({ ...previous, ...updated }));
      try {
        const refreshed = await request(`/student/complaints/${details.id}`);
        setDetails(refreshed);
        onDone(refreshed.followUpQuestion
          ? 'Thanks — the AI has one more question.'
          : 'Your report has been routed to the relevant team.');
      } catch (refreshError) {
        const message = `Your reply was saved, but report details could not be refreshed: ${refreshError.message}`;
        setError(message);
        onDone(message);
      }
    } catch (err) {
      setError(err.message);
      onDone(err.message);
    } finally {
      setBusy(false);
    }
  };

  const nextStatuses = {
    DEPARTMENT_ASSIGNED: ['PENDING_DEPARTMENT_REVIEW', 'REJECTED'],
    PENDING_DEPARTMENT_REVIEW: ['REJECTED'],
    WORKER_ASSIGNED: ['IN_PROGRESS'],
    IN_PROGRESS: ['ON_HOLD', 'WAITING_FOR_PARTS', 'WAITING_FOR_ACCESS', 'RESOLVED'],
    ON_HOLD: ['IN_PROGRESS'],
    WAITING_FOR_PARTS: ['IN_PROGRESS'],
    WAITING_FOR_ACCESS: ['IN_PROGRESS'],
    RESOLVED: ['CLOSED']
  }[details.status] || [];

  const statusLabel = status => status === 'RESOLVED' ? 'Completed'
    : status === 'IN_PROGRESS' ? 'Work in progress'
      : status.replaceAll('_', ' ');

  return <div className="modal-backdrop" onClick={onClose}>
    <div className="modal detail-modal" onClick={event => event.stopPropagation()}>
      <button className="close" onClick={onClose} aria-label="Close report details">×</button>
      <p className="eyebrow">REPORT #{details.id}</p>
      <h2>{details.title}</h2>
      <Status status={details.status} />

      <h3 className="timeline-title">Issue details</h3>
      <div className="report-detail-copy">
        <strong>Description</strong>
        <p className="modal-description">{details.description}</p>
        {details.additionalInformation && <>
          <strong>Additional information</strong>
          <p className="modal-description">{details.additionalInformation}</p>
        </>}
        {details.followUpQuestion && <div className="result-box">
          <strong>Information requested</strong>
          <p>{details.followUpQuestion}</p>
        </div>}
      </div>

      <div className="detail-grid">
        {details.studentName && <span>Student<strong>{details.studentName}</strong></span>}
        {details.studentEmail && <span>Student email<strong>{details.studentEmail}</strong></span>}
        {details.studentRollNo && <span>Roll number<strong>{details.studentRollNo}</strong></span>}
        <span>Department<strong>{details.departmentName || 'Awaiting routing'}</strong></span>
        <span>Assigned worker<strong>{details.assignedWorkerName
          || (details.workerAssigned ? 'Worker assigned' : 'Worker not assigned')}</strong></span>
        {details.assignedWorkerEmail && <span>Worker email<strong>{details.assignedWorkerEmail}</strong></span>}
        <span>Submitted<strong>{details.createdAt ? new Date(details.createdAt).toLocaleString() : '—'}</strong></span>
        <span>Last updated<strong>{details.updatedAt ? new Date(details.updatedAt).toLocaleString() : '—'}</strong></span>
        {details.resolvedAt && <span>Completed<strong>{new Date(details.resolvedAt).toLocaleString()}</strong></span>}
      </div>

      {(allowDepartmentUpdates || allowAdminUpdates) && <section className="report-workflow">
        <h3 className="timeline-title">{allowAdminUpdates ? 'Update report status' : 'Department workflow'}</h3>
        {allowDepartmentUpdates && (details.status === 'DEPARTMENT_ASSIGNED'
          || details.status === 'PENDING_DEPARTMENT_REVIEW') && <form className="inline-action" onSubmit={assignWorker}>
          <label>Assign this report to a department worker
            <select required value={workerId} onChange={event => setWorkerId(event.target.value)} disabled={busy}>
              <option value="">Select a worker</option>
              {workers.filter(worker => worker.enabled).map(worker =>
                <option key={worker.userId} value={worker.userId}>{worker.name}</option>)}
            </select>
          </label>
          <button className="primary small" disabled={busy || !workerId}>
            {busy ? 'Assigning...' : 'Mark worker assigned'}
          </button>
        </form>}
        {nextStatuses.length > 0 && <div className="status-actions">
          {nextStatuses.map(status => <button key={status}
            className={status === 'RESOLVED' ? 'primary small' : 'secondary'}
            disabled={busy} onClick={() => updateStatus(status)}>
            {busy ? 'Updating...' : statusLabel(status)}
          </button>)}
        </div>}
        {error && <p className="error" role="alert">{error}</p>}
      </section>}

      {allowFollowUp && details.followUpQuestion && <form className="inline-action" onSubmit={submitFollowUp}>
        <label>{details.followUpQuestion}
          <textarea required rows="2" maxLength={4000} value={followUp}
            onChange={event => setFollowUp(event.target.value)}
            placeholder="Add the missing detail" disabled={busy} />
        </label>
        {error && <p className="error" role="alert">{error}</p>}
        <button className="primary small" disabled={busy || !followUp.trim()}>
          <Send size={14} /> {busy ? 'Checking...' : 'Submit detail'}
        </button>
      </form>}

      {details.conversation?.length > 0 && <>
        <h3 className="timeline-title">Report conversation</h3>
        <div className="report-conversation">
          {details.conversation.map(message => <div className="report-conversation-message" key={message.id}>
            <div><strong>{message.senderType === 'STUDENT' ? 'Student' : 'CampusCare AI'}</strong>
              <time>{message.createdAt ? new Date(message.createdAt).toLocaleString() : ''}</time></div>
            <p>{message.message}</p>
          </div>)}
        </div>
      </>}

      <h3 className="timeline-title">Status history</h3>
      {details.history?.length
        ? details.history.map(record => <div className="timeline-row" key={record.id}>
          <span>{formatStatus(record.newStatus) || record.action}</span>
          <small>{record.description}{record.performedByName ? ` · ${record.performedByName}` : ''}</small>
          <time>{record.createdAt ? new Date(record.createdAt).toLocaleString() : ''}</time>
        </div>)
        : <p className="muted">No status updates have been recorded yet.</p>}
    </div>
  </div>;
}
