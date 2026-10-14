<template>
  <el-card class="surface-card" :class="{ 'surface-card-compact': compact }" shadow="never" :body-style="{ padding: '0' }">
    <template v-if="title || $slots.header || $slots.actions" #header>
      <div class="surface-card-header">
        <div><slot name="header"><h2>{{ title }}</h2></slot></div>
        <div v-if="$slots.actions" class="surface-card-actions"><slot name="actions" /></div>
      </div>
    </template>
    <div class="surface-card-body"><slot /></div>
    <template v-if="$slots.footer" #footer><slot name="footer" /></template>
  </el-card>
</template>

<script setup>
defineProps({ title: { type: String, default: '' }, compact: { type: Boolean, default: false } })
</script>

