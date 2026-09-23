/**
 * Bibliotech Circulation Systems - API Gateway Client
 * All requests route through Spring Cloud Gateway on http://localhost:8080
 */

const API_BASE = 'http://localhost:8080';

const Api = {
  // Token & Storage Management
  getToken() {
    return localStorage.getItem('bibliotech_token');
  },

  setToken(token) {
    if (token) {
      localStorage.setItem('bibliotech_token', token);
    } else {
      localStorage.removeItem('bibliotech_token');
    }
  },

  getUser() {
    try {
      const data = localStorage.getItem('bibliotech_user');
      return data ? JSON.parse(data) : null;
    } catch (e) {
      return null;
    }
  },

  setUser(user) {
    if (user) {
      localStorage.setItem('bibliotech_user', JSON.stringify(user));
    } else {
      localStorage.removeItem('bibliotech_user');
    }
  },

  clearSession() {
    localStorage.removeItem('bibliotech_token');
    localStorage.removeItem('bibliotech_user');
  },

  // Centralized Request Handler with Authorization Header Injection
  async request(endpoint, options = {}) {
    const url = `${API_BASE}${endpoint}`;
    const headers = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      ...(options.headers || {})
    };

    const token = this.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    try {
      const response = await fetch(url, {
        ...options,
        headers
      });

      // Handle 204 No Content
      if (response.status === 204) {
        return null;
      }

      const contentType = response.headers.get('content-type');
      let data = null;
      if (contentType && contentType.includes('application/json')) {
        data = await response.json();
      } else {
        data = await response.text();
      }

      if (!response.ok) {
        const errorMsg = (data && data.message) || (data && data.error) || response.statusText || 'Request failed';
        const err = new Error(errorMsg);
        err.status = response.status;
        err.data = data;
        throw err;
      }

      return data;
    } catch (err) {
      // Re-throw with descriptive context
      console.warn(`[API] ${options.method || 'GET'} ${endpoint} failed:`, err.message);
      throw err;
    }
  },

  // ==========================================
  // AUTH SERVICE ENDPOINTS (:8081 via Gateway)
  // ==========================================
  auth: {
    async login(username, password) {
      return Api.request('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password })
      });
    },

    async register(registerData) {
      return Api.request('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify(registerData)
      });
    },

    async validateToken() {
      return Api.request('/api/auth/validate');
    }
  },

  // ==========================================
  // BOOK SERVICE ENDPOINTS (:8082 via Gateway)
  // ==========================================
  books: {
    async getAll(search = '', category = '', branch = '') {
      const params = new URLSearchParams();
      if (search) params.append('search', search);
      if (category) params.append('category', category);
      if (branch) params.append('branch', branch);
      const query = params.toString() ? `?${params.toString()}` : '';
      return Api.request(`/api/books${query}`);
    },

    async getById(id) {
      return Api.request(`/api/books/${id}`);
    },

    async checkAvailability(id) {
      return Api.request(`/api/books/${id}/availability`);
    },

    async createBook(bookData) {
      return Api.request('/api/books', {
        method: 'POST',
        body: JSON.stringify(bookData)
      });
    },

    async updateBook(id, bookData) {
      return Api.request(`/api/books/${id}`, {
        method: 'PUT',
        body: JSON.stringify(bookData)
      });
    },

    async deleteBook(id) {
      return Api.request(`/api/books/${id}`, {
        method: 'DELETE'
      });
    }
  },

  // ==========================================
  // RENTAL SERVICE ENDPOINTS (:8083 via Gateway)
  // ==========================================
  rentals: {
    async borrow(bookId, loanDays = 14) {
      return Api.request('/api/rentals/borrow', {
        method: 'POST',
        body: JSON.stringify({ bookId, loanDays })
      });
    },

    async returnBook(rentalId) {
      return Api.request(`/api/rentals/${rentalId}/return`, {
        method: 'POST'
      });
    },

    async getMyRentals() {
      return Api.request('/api/rentals/my-rentals');
    },

    async getByStudentId(studentId) {
      return Api.request(`/api/rentals/student/${studentId}`);
    },

    async getAll() {
      return Api.request('/api/rentals');
    },

    async getOverdue() {
      return Api.request('/api/rentals/overdue');
    },

    // Simulation hook for live presentations: backdates due date to trigger overdue penalties
    async simulateOverdue(rentalId, days = 5) {
      return Api.request(`/api/rentals/${rentalId}/simulate-overdue?days=${days}`, {
        method: 'POST'
      });
    }
  },

  // ==========================================
  // FINE SERVICE ENDPOINTS (:8084 via Gateway)
  // ==========================================
  fines: {
    async getMyFines() {
      return Api.request('/api/fines/my-fines');
    },

    async getByStudentId(studentId) {
      return Api.request(`/api/fines/student/${studentId}`);
    },

    async getAll() {
      return Api.request('/api/fines');
    },

    async pay(fineId) {
      return Api.request(`/api/fines/${fineId}/pay`, {
        method: 'PUT'
      });
    }
  },

  // ==========================================
  // TOPOLOGY HEALTH CHECKS
  // ==========================================
  system: {
    async checkGateway() {
      try {
        const res = await fetch(`${API_BASE}/actuator/health`, { method: 'GET', mode: 'cors' });
        return res.ok;
      } catch (e) {
        // Fallback check books endpoint
        try {
          const res2 = await fetch(`${API_BASE}/api/books`, { method: 'GET', mode: 'cors' });
          return res2.ok || res2.status === 200 || res2.status === 401;
        } catch (e2) {
          return false;
        }
      }
    },

    async checkServiceDirect(port) {
      try {
        const res = await fetch(`http://localhost:${port}/actuator/health`, { method: 'GET', mode: 'no-cors' });
        return true; // no-cors resolves if port is responding
      } catch (e) {
        return false;
      }
    }
  }
};
