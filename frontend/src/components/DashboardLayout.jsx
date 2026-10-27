import { useState } from 'react';
import { Bell, Building2, CheckCircle2, ClipboardList, FilePlus2, HardHat, KeyRound, LayoutDashboard, LogOut, MessageCircle, RefreshCw, ShieldCheck, UserCog, Users } from 'lucide-react';
import OverviewPage from '../pages/OverviewPage';
import NewComplaintPage from '../pages/NewComplaintPage';
import ComplaintQueuePage from '../pages/ComplaintQueuePage';
import PeoplePage from '../pages/PeoplePage';
import NotificationsPage from '../pages/NotificationsPage';
import ChatPage from '../pages/ChatPage';
import AdminComplaintsPage from '../pages/AdminComplaintsPage';
import AdminDepartmentsPage from '../pages/AdminDepartmentsPage';
import DepartmentAdminsPage from '../pages/DepartmentAdminsPage';
import WorkersPage from '../pages/WorkersPage';
import StudentComplaintsPage from '../pages/StudentComplaintsPage';
import ChangePasswordPage from '../pages/ChangePasswordPage';
import '../dashboard.css';

export default function DashboardLayout({ user, onLogout }) {
  const [view, setView] = useState('overview'); const [toast, setToast] = useState(''); const [refresh, setRefresh] = useState(0);
  const [complaintGroup, setComplaintGroup] = useState('all');
  const navigate = (nextView, group = 'all') => {
    if (nextView === 'complaints') setComplaintGroup(group);
    setView(nextView);
  };
  const nav = user.role === 'ADMIN' ? [['overview', LayoutDashboard, 'Overview'], ['complaints', ClipboardList, 'All complaints'], ['departments', Building2, 'Departments'], ['admins', UserCog, 'Department admins'], ['workers', HardHat, 'Workers']] : user.role === 'STUDENT' ? [['overview', LayoutDashboard, 'My campus'], ['complaints', ClipboardList, 'My complaints'], ['new', FilePlus2, 'New complaint'], ['chat', MessageCircle, 'Ask CampusCare']] : user.role === 'DEPARTMENT_ADMIN' ? [['overview', LayoutDashboard, 'Department desk'], ['complaints', ClipboardList, 'Queue']] : [['overview', LayoutDashboard, 'My assignments']];
  const notify = (message) => { setToast(message); setTimeout(() => setToast(''), 3500); setRefresh(value => value + 1); };
  const page = view === 'overview' ? <OverviewPage user={user} refresh={refresh} onNavigate={navigate} /> : view === 'new' ? <NewComplaintPage onDone={notify} onNavigate={navigate} /> : view === 'complaints' && user.role === 'ADMIN' ? <AdminComplaintsPage initialGroup={complaintGroup} refresh={refresh} onDone={notify} /> : view === 'complaints' && user.role === 'STUDENT' ? <StudentComplaintsPage initialGroup={complaintGroup} refresh={refresh} onDone={notify} /> : view === 'complaints' ? <ComplaintQueuePage user={user} refresh={refresh} onDone={notify} /> : view === 'departments' ? <AdminDepartmentsPage refresh={refresh} onDone={notify} /> : view === 'admins' ? <DepartmentAdminsPage refresh={refresh} onDone={notify} /> : view === 'workers' ? <WorkersPage refresh={refresh} onDone={notify} /> : view === 'people' ? <PeoplePage refresh={refresh} onDone={notify} /> : view === 'chat' ? <ChatPage /> : view === 'password' ? <ChangePasswordPage onDone={notify} /> : <NotificationsPage />;
  return <div className="app-shell"><aside className="sidebar"><div className="side-brand"><div className="brand-mark small">CC<span>AI</span></div><span>CampusCare</span></div><div className="side-label">WORKSPACE</div><nav>{nav.map(([id, Icon, label]) => <button className={view === id ? 'active' : ''} key={id} onClick={() => navigate(id)}><Icon size={17} />{label}</button>)}</nav><div className="sidebar-bottom"><button className={view === 'notifications' ? 'active sidebar-link' : 'sidebar-link'} onClick={() => setView('notifications')}><Bell size={16} /> Notifications</button><button className={view === 'password' ? 'active sidebar-link' : 'sidebar-link'} onClick={() => setView('password')}><KeyRound size={16} /> Change password</button><div className="profile-chip"><div className="avatar">{user.name?.slice(0, 1)}</div><div><strong>{user.name}</strong><small>{user.role.replace('_', ' ')}</small></div></div><button className="logout" onClick={onLogout}><LogOut size={16} /> Sign out</button></div></aside><main className="content"><header className="topbar"><div><p className="eyebrow">{user.role.replace('_', ' ')} PORTAL</p><h2>{nav.find(item => item[0] === view)?.[2] || (view === 'password' ? 'Account security' : 'Notifications')}</h2></div><div className="top-actions"><span className="status-dot">System online</span><button className="icon-button" title="Refresh" onClick={() => setRefresh(value => value + 1)}><RefreshCw size={17} /></button><button className="icon-button" title="Notifications" onClick={() => setView('notifications')}><Bell size={17} /></button></div></header>{toast && <div className="toast"><CheckCircle2 size={17} /> {toast}</div>}{page}</main></div>;
}
