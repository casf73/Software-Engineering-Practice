<template>
  <div class="product-image" :class="{ 'product-image-small': small }">
    <img v-if="src && !failed" :src="assetUrl(src)" :alt="alt" @error="failed = true" />
    <div v-else class="product-image-placeholder">暂无商品图片</div>
    <slot name="overlay" />
  </div>
</template>
<script setup>
import { ref, watch } from 'vue'
import { assetUrl } from '../../api/http'
const props = defineProps({ src: { type: String, default: '' }, alt: { type: String, default: '商品图片' }, small: { type: Boolean, default: false } })
const failed = ref(false)
watch(() => props.src, () => { failed.value = false })
</script>

