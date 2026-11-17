<template>
  <div class="gift-preview" :class="[design.template, design.color]" aria-label="定制效果预览">
    <span v-if="symbols[design.motif]" class="gift-motif">{{ symbols[design.motif] }}</span>
    <span class="gift-text">{{ design.text || (design.motif === 'none' ? '在此预览定制文字' : '') }}</span>
  </div>
</template>

<script setup>
defineProps({ design: { type: Object, required: true } })
const symbols = { heart: '♥', star: '★', leaf: '❧' }
</script>

<style scoped>
.gift-preview { display: grid; align-content: center; justify-items: center; gap: 8px; min-height: 150px; padding: 20px; border: 3px double #bda88b; border-radius: 10px; background: #fff9ed; font-family: Georgia, serif; }
.gift-preview.modern { background: #edf5ff; border: 1px solid #a0b4d0; font-family: sans-serif; }
.gift-preview.craft { background: #f2e7d5; border: 2px dashed #ae8c61; }
.gift-motif { font-size: 34px; }
.gift-text { max-width: 100%; overflow-wrap: anywhere; text-align: center; }
.ink { color: #263247; }
.rose { color: #98234b; }
.forest { color: #21583d; }
</style>
