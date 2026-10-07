<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    :width="width"
    :close-on-click-modal="closeOnClickModal"
    :destroy-on-close="destroyOnClose"
    align-center
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <template v-if="$slots.title" #header><slot name="title" /></template>
    <div class="dialog-content"><slot /></div>
    <template #footer>
      <slot name="footer" :close="close"><el-button @click="close">关闭</el-button></slot>
    </template>
  </el-dialog>
</template>

<script setup>
defineProps({
  modelValue: { type: Boolean, required: true },
  title: { type: String, default: '' },
  width: { type: String, default: '520px' },
  closeOnClickModal: { type: Boolean, default: false },
  destroyOnClose: { type: Boolean, default: true },
})
const emit = defineEmits(['update:modelValue'])
function close() { emit('update:modelValue', false) }
</script>

