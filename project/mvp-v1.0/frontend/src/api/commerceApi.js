import http from './http'

export const commerceApi = {
  cart: () => http.get('/customer/cart'),
  quantity: (id, quantity) => http.put(`/customer/cart/${id}`, { quantity }),
  removeCart: id => http.delete(`/customer/cart/${id}`),
  favorites: () => http.get('/customer/favorites'),
  favorite: id => http.put(`/customer/favorites/${id}`),
  removeFavorite: id => http.delete(`/customer/favorites/${id}`),
  move: productIds => http.post('/customer/cart/favorites', { productIds }),
  checkout: (productIds, requestId) => http.post('/customer/checkout', { productIds, requestId }),
  customOrder: (id, design, requestId) => http.post(`/customer/products/${id}/custom-orders`, { design, requestId }),
  customization: id => http.get(`/public/products/${id}/customization`),
  sellerCustomization: id => http.get(`/seller/products/${id}/customization`),
  configure: (id, settings) => http.put(`/seller/products/${id}/customization`, settings),
  orders: (seller, page) => http.get(`/${seller ? 'seller' : 'customer'}/shop-orders`, { params: { page } }),
  advance: (id, status) => http.put(`/seller/shop-orders/${id}/status`, { status }),
  cancel: (seller, id) => http.post(`/${seller ? 'seller' : 'customer'}/shop-orders/${id}/cancel`),
}

export function newOrderRequestId() {
  return Array.from(crypto.getRandomValues(new Uint8Array(16)), byte => byte.toString(16).padStart(2, '0')).join('')
}
