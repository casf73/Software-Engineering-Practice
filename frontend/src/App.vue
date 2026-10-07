<template>
  <el-config-provider :locale="zhCn">
    <div class="app-shell">
      <AppHeader />
      <main class="app-main"><router-view /></main>
    </div>
  </el-config-provider>
</template>

<script setup>
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import AppHeader from './components/common/AppHeader.vue'
</script>

