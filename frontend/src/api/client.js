const BASE_URL = '/api/v1';

export const getAuthToken = () => localStorage.getItem('bookstore_token');
export const setAuthToken = (token) => localStorage.setItem('bookstore_token', token);
export const removeAuthToken = () => localStorage.removeItem('bookstore_token');

async function request(endpoint, options = {}) {
  const token = getAuthToken();
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const response = await fetch(`${BASE_URL}${endpoint}`, { ...options, headers });

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

const json = (method, body) => ({ method, body: body === undefined ? undefined : JSON.stringify(body) });

function toQuery(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, val]) => {
    if (val !== undefined && val !== null && val !== '') query.append(key, val);
  });
  return query.toString();
}

export const api = {
  // Auth
  register: (payload) => request('/auth/register', json('POST', payload)),
  login: (payload) => request('/auth/login', json('POST', payload)),
  getMe: () => request('/auth/me'),

  // Catalog
  getCategories: () => request('/categories'),
  getPublishers: () => request('/publishers'),
  getFacets: () => request('/books/facets'),
  getShelves: (limit = 4) => request(`/books/shelves?limit=${limit}`),
  getBooks: (params = {}) => request(`/books?${toQuery(params)}`),
  getBookById: (id) => request(`/books/${id}`),
  getRelatedBooks: (id, limit = 4) => request(`/books/${id}/related?limit=${limit}`),
  addReview: (id, payload) => request(`/books/${id}/reviews`, json('POST', payload)),

  // Recommendations
  getHistoryRecommendations: (limit = 6) => request(`/recommendations/history?limit=${limit}`),

  // Wishlist and writers
  getWishlist: () => request('/wishlist'),
  addToWishlist: (bookId) => request(`/wishlist/${bookId}`, { method: 'PUT' }),
  removeFromWishlist: (bookId) => request(`/wishlist/${bookId}`, { method: 'DELETE' }),
  getWriters: (followed = false) => request(`/writers?followed=${followed}`),
  getWriter: (id) => request(`/writers/${id}`),
  followWriter: (id) => request(`/writers/${id}/follow`, { method: 'PUT' }),
  unfollowWriter: (id) => request(`/writers/${id}/follow`, { method: 'DELETE' }),

  // User, points and wallet
  getAddresses: () => request('/users/addresses'),
  addAddress: (payload) => request('/users/addresses', json('POST', payload)),
  getGiftPoints: () => request('/users/points'),
  getWallet: () => request('/wallet'),
  topUpWallet: (amount) => request('/wallet/top-up', json('POST', { amount })),
  getCoupons: () => request('/coupons'),

  // Cart
  getCart: () => request('/cart'),
  addToCart: (bookId, quantity = 1) => request('/cart', json('POST', { bookId, quantity })),
  updateCartItem: (itemId, quantity) => request(`/cart/items/${itemId}`, json('PUT', { quantity })),
  removeCartItem: (itemId) => request(`/cart/items/${itemId}`, { method: 'DELETE' }),
  clearCart: () => request('/cart', { method: 'DELETE' }),

  // Orders and 48 hour cancellation
  quoteOrder: (payload) => request('/orders/quote', json('POST', payload)),
  placeOrder: (payload) => request('/orders', json('POST', payload)),
  getOrderHistory: () => request('/orders'),
  getOrderById: (orderId) => request(`/orders/${orderId}`),
  cancelOrder: (orderId, reason) => request(`/orders/${orderId}/cancel`, json('POST', { reason })),
  buyItAgain: (orderId) => request(`/orders/${orderId}/reorder`, { method: 'POST' }),
};
