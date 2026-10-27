import { useEffect, useState } from 'react';
import { Bell } from 'lucide-react';
import { request } from '../api';
import { EmptyState } from '../components/ui';

export default function NotificationsPage() {
  const [items, setItems] = useState([]);
  const load = () => request('/notifications').then(setItems).catch(() => {});
  useEffect(load, []);
  const markRead = async id => { try { await request(`/notifications/${id}/read`, { method: 'PUT' }); load(); } catch { /* keep notification visible on transient errors */ } };
  return <div className="page narrow"><div className="hero-row"><div><p className="eyebrow">NOTIFICATIONS</p><h1>Signals worth seeing.</h1></div></div><section className="panel">{items.map(item => <div className={`notification-row ${item.read ? 'read' : 'unread'}`} key={item.id}><Bell size={17} /><div><strong>{item.message}</strong><span>{item.createdAt ? new Date(item.createdAt).toLocaleString() : 'Just now'}</span></div>{!item.read && <button className="secondary" onClick={() => markRead(item.id)}>Mark read</button>}</div>)}{!items.length && <EmptyState text="You are all caught up." />}</section></div>;
}
