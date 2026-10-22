import http from './http'

export const sellerApi = {
  login: (payload) => http.post('/seller/login', payload),
  logout: () => http.post('/seller/logout'),
  session: () => http.get('/seller/session'),
  changePassword: (payload) => http.put('/seller/password', payload),
  products: (params) => http.get('/seller/products', { params }),
  categories: () => http.get('/public/categories'),
  saveCategory: (id, body) => id ? http.put('/seller/categories/' + id, body) : http.post('/seller/categories', body),
  deleteCategory: (id) => http.delete('/seller/categories/' + id),
  relistProduct: (id) => http.post(`/seller/products/${id}/relist`),
  freezeProduct: (id) => http.post(`/seller/products/${id}/freeze`),
  unfreezeProduct: (id) => http.post(`/seller/products/${id}/unfreeze`),
  workbench: (id) => http.get(`/seller/products/${id}/workbench`),
  createProduct: (payload) => http.post('/seller/products', payload),
  updateProduct: (id, payload) => http.put(`/seller/products/${id}`, payload),
  uploadImage: (file) => {
    const data = new FormData()
    data.append('file', file)
    return http.post('/seller/products/image', data)
  },
  startTrade: (id) => http.post(`/seller/products/${id}/trades/start`),
  finishTrade: (intentId, payload) => http.post('/seller/trades/' + intentId + '/result', payload),
  processIntent: (intentId, payload) => http.post('/seller/intents/' + intentId + '/action', payload),
  delistProduct: (id) => http.post(`/seller/products/${id}/delist`),
}

