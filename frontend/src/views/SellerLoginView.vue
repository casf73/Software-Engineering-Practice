<template>
  <div class="login-shell">
    <AppCard class="login-card">
      <div class="login-icon">店</div>
      <p class="eyebrow">卖家工作台</p>
      <h1>欢迎回来</h1>
      <p class="login-intro">登录后发布商品、管理排队意向并登记交易结果。</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="login">
        <el-form-item label="账号" prop="username"><el-input v-model="form.username" size="large" autocomplete="username" placeholder="请输入卖家账号" /></el-form-item>
        <el-form-item label="密码" prop="password"><el-input v-model="form.password" size="large" type="password" show-password autocomplete="current-password" placeholder="请输入密码" @keyup.enter="login" /></el-form-item>
        <el-button type="primary" size="large" class="full-button" :loading="loading" @click="login">登录工作台</el-button>
      </el-form>
      <p class="login-footnote">初始账号：admin　·　初始密码：123456</p>
    </AppCard>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppCard from '../components/common/AppCard.vue'
import { apiErrorMessage } from '../api/http'
import { sellerApi } from '../api/sellerApi'

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}
const router = useRouter()
const route = useRoute()

async function login() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await sellerApi.login(form)
    ElMessage.success('登录成功')
    router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/seller/dashboard')
  } catch (error) { ElMessage.error(apiErrorMessage(error, '登录失败')) }
  finally { loading.value = false }
}
</script>
