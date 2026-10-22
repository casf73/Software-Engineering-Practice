<template>
  <div class="product-gallery">
    <ProductImage :src="images[index] || ''" :alt="alt"><template #overlay><slot name="overlay" /></template></ProductImage>
    <div v-if="images.length > 1" class="gallery-thumbs" aria-label="选择商品图片">
      <button v-for="(image, i) in images" :key="image + i" type="button" :class="{ selected: index === i }" :aria-label="'第 ' + (i + 1) + ' 张图片'" :aria-pressed="index === i" @click="index = i"><img :src="image" :alt="alt + ' ' + (i + 1)"></button>
    </div>
  </div>
</template>
<script setup>
import { ref, watch } from 'vue'
import ProductImage from './ProductImage.vue'
const props = defineProps({ images: { type: Array, default: () => [] }, alt: { type: String, default: '商品图片' } })
const index = ref(0)
watch(() => props.images, () => { index.value = 0 })
</script>
