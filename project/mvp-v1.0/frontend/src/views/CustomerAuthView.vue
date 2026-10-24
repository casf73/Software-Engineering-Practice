<template>
  <div class="content-width customer-auth-page">
    <PageHeader :title="registering ? '客户注册' : '客户登录'" description="登录后提交的购买意向会保存在自己的购买记录中。" />
    <AppCard class="customer-auth-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名" prop="username"><el-input v-model="form.username" maxlength="32" autocomplete="username" placeholder="3～32 位文字、数字、下划线或短横线" /></el-form-item>
        <el-form-item label="密码" prop="password"><el-input v-model="form.password" type="password" show-password maxlength="128" :autocomplete="registering ? 'new-password' : 'current-password'" /></el-form-item>
        <template v-if="registering">
          <el-form-item label="确认密码" prop="confirmation"><el-input v-model="form.confirmation" type="password" show-password maxlength="128" autocomplete="new-password" /></el-form-item>
          <el-form-item label="联系电话" prop="phone"><el-input v-model="form.phone" maxlength="30" autocomplete="tel" /></el-form-item>
          <el-form-item label="默认交易地点" prop="defaultLocation"><el-input v-model="form.defaultLocation" maxlength="200" placeholder="例如：学校南门；提交购买意向时可调整" /></el-form-item>
        </template>
        <el-button native-type="submit" type="primary" class="full-button" :loading="submitting">{{ registering ? '注册并登录' : '登录' }}</el-button>
      </el-form>
      <div class="customer-auth-links">
        <RouterLink :to="{ path: registering ? '/customer/login' : '/customer/register', query: route.query }">{{ registering ? '已有账号，去登录' : '没有账号，去注册' }}</RouterLink>
        <RouterLink to="/">继续浏览商品</RouterLink>
      </div>
    </AppCard>
  </div>
</template>
<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppCard from '../components/common/AppCard.vue'
import PageHeader from '../components/common/PageHeader.vue'
import { customerApi } from '../api/customerApi'
import { apiErrorMessage } from '../api/http'
import { setCustomerSession } from '../stores/customerSession'

const route = useRoute(), router = useRouter()
const registering = computed(() => route.name === 'customer-register')
const formRef = ref(), submitting = ref(false)
const form = reactive({ username: '', password: '', confirmation: '', phone: '', defaultLocation: '' })
const rules = computed(() => ({
  username: [{ required: true, whitespace: true, message: '请输入用户名', trigger: 'blur' }, ...(registering.value ? [{ pattern: /^[\p{L}\p{N}_-]{3,32}$/u, message: '用户名须为 3～32 位文字、数字、下划线或短横线', trigger: 'blur' }] : [])],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, ...(registering.value ? [{ min: 8, max: 128, message: '密码须为 8～128 位', trigger: 'blur' }] : [])],
  confirmation: [{ validator: (_rule, value, callback) => callback(value === form.password ? undefined : new Error('两次密码不一致')), trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }, { pattern: /^(?=.*[0-9])[0-9+() -]{6,30}$/, message: '请输入有效联系电话', trigger: 'blur' }],
  defaultLocation: [{ required: true, whitespace: true, max: 200, message: '请输入不超过 200 字的默认交易地点', trigger: 'blur' }],
}))
watch(registering, async () => {
  form.password = ''; form.confirmation = ''
  await nextTick(); formRef.value?.clearValidate()
})
async function submit() {
  if (submitting.value) return
  if (!await formRef.value?.validate().catch(() => false)) return
  submitting.value = true
  const wasRegistering = registering.value
  try {
    const payload = { username: form.username.trim(), password: form.password }
    if (wasRegistering) Object.assign(payload, { phone: form.phone.trim(), defaultLocation: form.defaultLocation.trim() })
    const { data } = await (wasRegistering ? customerApi.register(payload) : customerApi.login(payload))
    setCustomerSession(data)
    form.password = ''; form.confirmation = ''
    ElMessage.success(wasRegistering ? '注册成功，已登录' : '登录成功')
    const target = route.query.redirect
    await router.replace(typeof target === 'string' && target.startsWith('/') && !target.startsWith('//') && !target.startsWith('/customer/login') && !target.startsWith('/customer/register') ? target : '/customer/account')
  } catch (error) { ElMessage.error(apiErrorMessage(error, wasRegistering ? '注册失败' : '登录失败')) }
  finally { submitting.value = false }
}
</script>
