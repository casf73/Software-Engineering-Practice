<template><el-tag :type="tagType" effect="plain">{{ label }}</el-tag></template>
<script setup>
import { computed } from 'vue'
import { intentLabel, intentTagType, productLabel, productTagType } from '../../utils/status'
const props = defineProps({ status: { type: String, required: true }, kind: { type: String, default: 'intent' } })
const label = computed(() => props.kind === 'product' ? productLabel(props.status) : intentLabel(props.status))
const tagType = computed(() => props.kind === 'product' ? productTagType(props.status) : intentTagType(props.status))
</script>

