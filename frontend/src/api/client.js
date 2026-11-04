const BASE_URL = '/api/v1';

export const getAuthToken = () => localStorage.getItem('bookstore_token');
export const setAuthToken = (token) => localStorage.setItem('bookstore_token', token);
export const removeAuthToken = () => localStorage.removeItem('bookstore_token');

async function request(endpoint, options = {}) {
  const token = getAuthToken();
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const response = await fetch(`${BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (response.status === 204) {
    return null;
  }

  const data = await response.json().catch(() => ({}));

  if (!response.ok) {
    const error = new Error(data.message || `Request failed with status ${response.status}`);
    error.status = response.status;
    error.data = data;
    throw error;
  }

  return data;
}

export const api = {
  // Auth
  register: (payload) => request('/auth/register', { method: 'POST', body: JSON.stringify(payload) }),
  login: (payload) => request('/auth/login', { method: 'POST', body: JSON.stringify(payload) }),
  getMe: () => request('/auth/me'),

  // Catalog
  getCategories: () => request('/categories'),
  getPublishers: () => request('/publishers'),
  getBooks: (params = {}) => {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([key, val]) => {
      if (val !== undefined && val !== null && val !== '') {
        query.append(key, val);
      }
    });
    return request(`/books?${query.toString()}`);
  },
  getBookById: (id) => request(`/books/${id}`),
  getRelatedBooks: (id, limit = 4) => request(`/books/${id}/related?limit=${limit}`),

  // Recommendations
  getHistoryRecommendations: (limit = 6) => request(`/recommendations/history?limit=${limit}`),

  // User & Points
  getAddresses: () => request('/users/addresses'),
  addAddress: (payload) => request('/users/addresses', { method: 'POST', body: JSON.stringify(payload) }),
  getGiftPoints: () => request('/users/points'),

  // Cart
  getCart: () => request('/cart'),
  addToCart: (bookId, quantity = 1) => request('/cart', { method: 'POST', body: JSON.stringify({ bookId, quantity }) }),
  updateCartItem: (itemId, quantity) => request(`/cart/items/${itemId}`, { method: 'PUT', body: JSON.stringify({ quantity }) }),
  removeCartItem: (itemId) => request(`/cart/items/${itemId}`, { method: 'DELETE' }),
  clearCart: () => request('/cart', { method: 'DELETE' }),

  // Orders & 48h Cancellation
  placeOrder: (payload) => request('/orders', { method: 'POST', body: JSON.stringify(payload) }),
  getOrderHistory: () => request('/orders'),
  getOrderById: (orderId) => request(`/orders/${orderId}`),
  cancelOrder: (orderId, reason) => request(`/orders/${orderId}/cancel`, { method: 'POST', body: JSON.stringify({ reason }) }),
  buyItAgain: (orderId) => request(`/orders/${orderId}/reorder`, { method: 'POST' }),
};
