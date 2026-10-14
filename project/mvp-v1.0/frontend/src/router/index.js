import { createRouter, createWebHistory } from 'vue-router'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'storefront', component: () => import('../views/StorefrontView.vue') },
    { path: '/seller/login', name: 'seller-login', component: () => import('../views/SellerLoginView.vue') },
    { path: '/seller/dashboard', name: 'seller-dashboard', component: () => import('../views/SellerDashboardView.vue'), meta: { sellerOnly: true } },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

