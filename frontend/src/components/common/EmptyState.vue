<template>
  <div class="empty-state">
    <span class="empty-state-mark">{{ mark }}</span>
    <strong>{{ title }}</strong>
    <p>{{ description }}</p>
    <slot />
  </div>
</template>
<script setup>
defineProps({
  title: { type: String, default: '这里暂时没有内容' },
  description: { type: String, default: '' },
  mark: { type: String, default: '—' },
})
</script>

