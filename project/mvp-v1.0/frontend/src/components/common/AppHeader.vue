<template>
  <header class="site-header">
    <div class="header-inner">
      <RouterLink class="brand" to="/" aria-label="返回商品首页">在线购物系统</RouterLink>
      <nav class="header-nav" aria-label="主导航">
        <RouterLink to="/" class="nav-link">商品</RouterLink>
        <RouterLink v-if="customerSession.customer" to="/customer/account" class="nav-link">我的购买 · {{ customerSession.customer.username }}</RouterLink>
        <RouterLink v-else to="/customer/login" class="nav-link">客户登录 / 注册</RouterLink>
        <RouterLink to="/seller/login" class="nav-link">卖家后台</RouterLink>
      </nav>
    </div>
  </header>
</template>


<script setup>
import { onMounted } from 'vue'
import { customerSession, loadCustomerSession } from '../../stores/customerSession'
onMounted(() => { loadCustomerSession().catch(() => {}) })
</script>
