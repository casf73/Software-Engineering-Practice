<template>
  <div class="empty-state">
    <el-empty :description="title" :image-size="72" />
    <p v-if="description">{{ description }}</p>
    <slot />
  </div>
</template>
<script setup>
defineProps({
  title: { type: String, default: '这里暂时没有内容' },
  description: { type: String, default: '' },
})
</script>

