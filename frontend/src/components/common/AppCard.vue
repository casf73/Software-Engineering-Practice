<template>
  <section class="surface-card" :class="{ 'surface-card-compact': compact }">
    <header v-if="title || $slots.header || $slots.actions" class="surface-card-header">
      <div><slot name="header"><h2>{{ title }}</h2></slot></div>
      <div v-if="$slots.actions" class="surface-card-actions"><slot name="actions" /></div>
    </header>
    <div class="surface-card-body"><slot /></div>
    <footer v-if="$slots.footer" class="surface-card-footer"><slot name="footer" /></footer>
  </section>
</template>

<script setup>
defineProps({ title: { type: String, default: '' }, compact: { type: Boolean, default: false } })
</script>

