<template>
  <div class="content-width seller-layout">
    <aside class="seller-sidebar" aria-label="卖家后台目录">
      <div class="seller-sidebar-title">卖家后台</div>
      <el-menu :default-active="ordersPage ? 'orders' : activeSection" @select="selectMenu">
        <el-menu-item v-for="item in menuItems" :key="item.key" :index="item.key">{{ item.label }}</el-menu-item>
      </el-menu>
    </aside>
    <div class="seller-content">
      <RouterView v-slot="{ Component }">
        <component :is="Component" v-if="ordersPage" />
        <component :is="Component" v-else v-model:active-section="activeSection" />
      </RouterView>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const menuItems = [
  { key: 'overview', label: '概览' },
  { key: 'product', label: '商品管理' },
  { key: 'queue', label: '购买意向' },
  { key: 'customers', label: '客户管理' },
  { key: 'categories', label: '分类管理' },
  { key: 'account', label: '账号管理' },
  { key: 'orders', label: '订单管理' },
]

const route = useRoute()
const router = useRouter()
const activeSection = ref('overview')
const ordersPage = computed(() => route.name === 'seller-orders')

function selectMenu(key) {
  if (key === 'orders') {
    if (!ordersPage.value) return router.push({ name: 'seller-orders' })
    return
  }
  activeSection.value = key
  if (ordersPage.value) return router.push({ name: 'seller-dashboard' })
}
</script>
