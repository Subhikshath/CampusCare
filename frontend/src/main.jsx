import { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import LoginPage from './pages/LoginPage';
import DashboardLayout from './components/DashboardLayout';
import './styles.css';

function App() {
  const [user, setUser] = useState(null);
  useEffect(() => {
    const saved = localStorage.getItem('campuscare_user');
    if (saved) setUser(JSON.parse(saved));
  }, []);
  const login = profile => { localStorage.setItem('campuscare_user', JSON.stringify(profile)); setUser(profile); };
  const logout = () => { localStorage.removeItem('campuscare_token'); localStorage.removeItem('campuscare_user'); setUser(null); };
  return user ? <DashboardLayout user={user} onLogout={logout} /> : <LoginPage onLogin={login} />;
}

createRoot(document.getElementById('root')).render(<App />);
