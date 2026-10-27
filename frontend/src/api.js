const API = import.meta.env.VITE_API_URL ?? (import.meta.env.DEV ? 'http://localhost:8080' : '');

export const demoAccounts = import.meta.env.DEV ? {
  ADMIN: ['admin@college.edu', 'admin12345'],
  STUDENT: ['student@college.edu', 'student123'],
  DEPARTMENT_ADMIN: ['department-admin@college.edu', 'admin12345'],
  WORKER: ['worker@college.edu', 'worker123']
} : {};

export async function request(path, options = {}) {
  const token = localStorage.getItem('campuscare_token');
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };
  if (token) headers.Authorization = `Bearer ${token}`;
  const response = await fetch(`${API}${path}`, { ...options, headers });
  const text = await response.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  if (!response.ok) throw new Error(data?.message || data?.error || `Request failed (${response.status})`);
  return data;
}
