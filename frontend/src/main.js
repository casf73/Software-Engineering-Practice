import { createApp } from 'vue'
import {
  ElButton, ElConfigProvider, ElDialog, ElForm, ElFormItem, ElInput,
  ElRadio, ElRadioGroup, ElSkeleton, ElTable, ElTableColumn, ElTag,
} from 'element-plus'
import 'element-plus/theme-chalk/base.css'
import 'element-plus/theme-chalk/el-button.css'
import 'element-plus/theme-chalk/el-config-provider.css'
import 'element-plus/theme-chalk/el-dialog.css'
import 'element-plus/theme-chalk/el-overlay.css'
import 'element-plus/theme-chalk/el-form.css'
import 'element-plus/theme-chalk/el-form-item.css'
import 'element-plus/theme-chalk/el-input.css'
import 'element-plus/theme-chalk/el-loading.css'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-radio.css'
import 'element-plus/theme-chalk/el-radio-group.css'
import 'element-plus/theme-chalk/el-scrollbar.css'
import 'element-plus/theme-chalk/el-skeleton.css'
import 'element-plus/theme-chalk/el-table.css'
import 'element-plus/theme-chalk/el-tag.css'
import App from './App.vue'
import router from './router'
import './styles.css'

const app = createApp(App)
app.use(router)
;[ElButton, ElConfigProvider, ElDialog, ElForm, ElFormItem, ElInput,
  ElRadio, ElRadioGroup, ElSkeleton, ElTable, ElTableColumn, ElTag]
  .forEach((component) => app.use(component))
app.mount('#app')

