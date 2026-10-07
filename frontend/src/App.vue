<template>
  <el-config-provider :locale="zhCn">
    <div class="app-shell">
      <AppHeader />
      <main class="app-main"><router-view /></main>
      <footer class="app-footer">
        <span>拾物 · 单卖家交易管理</span>
        <span>线下完成交易，线上管理队列</span>
      </footer>
    </div>
  </el-config-provider>
</template>

<script setup>
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import AppHeader from './components/common/AppHeader.vue'
</script>

