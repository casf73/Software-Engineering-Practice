<template>
  <div class="product-gallery">
    <ProductImage :src="images[activeIndex] || ''" :alt="alt">
      <template #overlay><slot name="overlay" /></template>
    </ProductImage>
    <div v-if="images.length > 1" class="gallery-thumbnails" aria-label="商品图片">
      <button
        v-for="(image, index) in images"
        :key="image + index"
        type="button"
        :class="{ active: activeIndex === index }"
        :aria-label="'查看第 ' + (index + 1) + ' 张商品图片'"
        @click="activeIndex = index"
      >
        <img :src="assetUrl(image)" :alt="alt + ' ' + (index + 1)" />
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { assetUrl } from '../../api/http'
import ProductImage from './ProductImage.vue'

const props = defineProps({
  images: { type: Array, default: () => [] },
  alt: { type: String, default: '商品图片' },
})
const activeIndex = ref(0)
watch(() => props.images, () => { activeIndex.value = 0 })
</script>
