import http from './http'

export const commerceApi = {
  // 购物车
  cart: () => http.get('/customer/cart'),
  quantity: (productId, quantity) => http.put(`/customer/cart/${productId}`, { quantity }),
  removeCart: productId => http.delete(`/customer/cart/${productId}`),
  move: productIds => http.post('/customer/cart/favorites', { productIds }),

  // 收藏夹
  favorites: () => http.get('/customer/favorites'),
  favorite: productId => http.put(`/customer/favorites/${productId}`),
  removeFavorite: productId => http.delete(`/customer/favorites/${productId}`),

  // 下单
  checkout: (productIds, requestId) => http.post('/customer/checkout', { requestId, productIds }),
  customOrder: (productId, design, requestId) =>
    http.post(`/customer/products/${productId}/custom-orders`, { requestId, design }),
  customization: productId => http.get(`/public/products/${productId}/customization`),

  // 商城订单
  orders: params => http.get('/customer/shop-orders', { params }),
  cancelOrder: id => http.post(`/customer/shop-orders/${id}/cancel`),
}

// 幂等键：后端要求 [A-Za-z0-9_-]{8,64}
export function newOrderRequestId() {
  const bytes = new Uint8Array(16)
  if (globalThis.crypto?.getRandomValues) globalThis.crypto.getRandomValues(bytes)
  else for (let i = 0; i < bytes.length; i++) bytes[i] = Math.floor(Math.random() * 256)
  return Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('')
}
