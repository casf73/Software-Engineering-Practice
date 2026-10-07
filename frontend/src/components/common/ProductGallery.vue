<template>
  <div class="product-gallery">
    <ProductImage :src="images[0] || ''" :alt="alt">
      <template #overlay><slot name="overlay" /></template>
    </ProductImage>
  </div>
</template>

<script setup>
import ProductImage from './ProductImage.vue'

defineProps({
  images: { type: Array, default: () => [] },
  alt: { type: String, default: '商品图片' },
})
</script>
