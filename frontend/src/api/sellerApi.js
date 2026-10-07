import http from './http'

export const sellerApi = {
  login: (payload) => http.post('/seller/login', payload),
  logout: () => http.post('/seller/logout'),
  session: () => http.get('/seller/session'),
  workbench: () => http.get('/seller/workbench'),
  createProduct: (payload) => http.post('/seller/products', payload),
  updateProduct: (payload) => http.put('/seller/products/current', payload),
  uploadImage: (file) => {
    const data = new FormData()
    data.append('file', file)
    return http.post('/seller/products/image', data)
  },
  startTrade: () => http.post('/seller/trades/start'),
  finishTrade: (intentId, payload) => http.post('/seller/trades/' + intentId + '/result', payload),
  processIntent: (intentId, payload) => http.post('/seller/intents/' + intentId + '/action', payload),
  delistProduct: () => http.post('/seller/products/current/delist'),
}

