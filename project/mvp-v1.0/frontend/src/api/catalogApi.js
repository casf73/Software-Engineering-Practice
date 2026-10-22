import http from './http'
export const catalogApi = {
 products: (params) => http.get('/public/products', { params }),
 product: (id) => http.get('/public/products/' + id),
 categories: () => http.get('/public/categories'),
 submit: (id, payload) => http.post(`/public/products/${id}/intents`, payload),
}
