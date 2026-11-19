<template>
  <header class="site-header">
    <div class="header-inner">
      <RouterLink v-if="sellerArea" class="brand" :to="sellerHome">在线购物系统 · 卖家后台</RouterLink>
      <RouterLink v-else class="brand" to="/" aria-label="返回商品首页">在线购物系统</RouterLink>
      <nav v-if="sellerArea" class="header-nav" aria-label="卖家导航">
        <RouterLink v-if="sellerLogin" to="/seller/login" class="nav-link">卖家登录</RouterLink>
        <template v-else>
          <RouterLink to="/seller/dashboard" class="nav-link">卖家工作台</RouterLink>
          <RouterLink to="/seller/shop-orders" class="nav-link">订单管理</RouterLink>
        </template>
        <RouterLink to="/" class="nav-link">返回商品页</RouterLink>
      </nav>
      <nav v-else class="header-nav" aria-label="主导航">
        <RouterLink to="/" class="nav-link">商品</RouterLink>
         <RouterLink :to="{ path: '/', query: { customizable: '1' } }" class="nav-link">在线定制</RouterLink>
        <RouterLink to="/customization" class="nav-link">在线定制</RouterLink>
        <RouterLink v-if="customerSession.customer" to="/customer/account" class="nav-link">我的购买 · {{ customerSession.customer.username }}</RouterLink>
        <RouterLink v-else to="/customer/login" class="nav-link">客户登录 / 注册</RouterLink>
        <template v-if="customerSession.customer">
          <RouterLink to="/customer/cart" class="nav-link">购物车</RouterLink>
          <RouterLink to="/customer/favorites" class="nav-link">收藏夹</RouterLink>
          <RouterLink to="/customer/shop-orders" class="nav-link">订单</RouterLink>
        </template>
        <RouterLink to="/seller/login" class="nav-link">卖家后台</RouterLink>
      </nav>
    </div>
  </header>
</template>


<script setup>
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { customerSession, loadCustomerSession } from '../../stores/customerSession'

const route = useRoute()
const sellerArea = computed(() => route.meta.sellerArea === true)
const sellerLogin = computed(() => route.name === 'seller-login')
const sellerHome = computed(() => sellerLogin.value ? '/seller/login' : '/seller/dashboard')

const buyerPage = computed(() => route.matched.length > 0 && !sellerArea.value)
watch(buyerPage, isBuyer => {
  if (isBuyer) loadCustomerSession().catch(() => {})
}, { immediate: true })
</script>
