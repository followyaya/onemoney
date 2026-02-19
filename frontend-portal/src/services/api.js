import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8001',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;

export const apiKeyService = {
  getAll: () => api.get('/api-keys'),
  create: (data) => api.post('/api-keys', data),
  delete: (id) => api.delete(`/api-keys/${id}`),
  rotate: (id) => api.post(`/api-keys/${id}/rotate`),
};

export const analyticsService = {
  getMetrics: (range = '24h') => api.get(`/analytics?range=${range}`),
  getTransactions: (params) => api.get('/analytics/transactions', { params }),
  getPartners: () => api.get('/analytics/partners'),
};
