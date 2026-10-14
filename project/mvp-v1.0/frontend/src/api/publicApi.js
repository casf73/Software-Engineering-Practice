import http from './http'

export const publicApi = {
  getCurrentProduct: () => http.get('/public/products/current'),
  submitIntent: (payload) => http.post('/public/intents', payload),
  getIntent: (passcode) => http.get('/public/intents/' + encodeURIComponent(passcode)),
  cancelIntent: (passcode) => http.delete('/public/intents/' + encodeURIComponent(passcode)),
}

