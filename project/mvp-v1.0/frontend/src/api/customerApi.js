import http from './http'

export const customerApi = {
  register: body => http.post('/customer/register', body),
  login: body => http.post('/customer/login', body),
  session: () => http.get('/customer/session'),
  logout: () => http.post('/customer/logout'),
  orders: params => http.get('/customer/orders', { params }),
  submitIntent: (id, body) => http.post(`/customer/products/${id}/intents`, body),
}
