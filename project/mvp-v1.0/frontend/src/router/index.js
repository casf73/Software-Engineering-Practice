import { createRouter, createWebHistory } from 'vue-router'
import { loadCustomerSession } from '../stores/customerSession'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'storefront', component: () => import('../views/StorefrontView.vue') },
    { path: '/customer/login', name: 'customer-login', component: () => import('../views/CustomerAuthView.vue') },
    { path: '/customer/register', name: 'customer-register', component: () => import('../views/CustomerAuthView.vue') },
    { path: '/customer/account', name: 'customer-account', component: () => import('../views/CustomerAccountView.vue'), meta: { customerOnly: true } },
    { path: '/seller/login', name: 'seller-login', component: () => import('../views/SellerLoginView.vue') },
    { path: '/seller/dashboard', name: 'seller-dashboard', component: () => import('../views/SellerDashboardView.vue'), meta: { sellerOnly: true } },
  ],
  scrollBehavior: () => ({ top: 0 }),
})
router.beforeEach(async to => {
  if (!to.meta.customerOnly) return true
  try { if (await loadCustomerSession(true)) return true } catch { /* 登录页允许重新建立会话 */ }
  return { name: 'customer-login', query: { redirect: to.fullPath } }
})
export default router
