import http from './http'
export const catalogApi = {
 products: (params) => http.get('/public/products', { params }),
 searchProducts: (keyword, categoryId) => http.post('/public/product-search', { keyword, categoryId }),
 hotSearches: () => http.get('/public/product-search/hot'),
 product: (id) => http.get('/public/products/' + id),
 categories: () => http.get('/public/categories'),
 submit: (id, payload) => http.post(`/public/products/${id}/intents`, payload),
}
