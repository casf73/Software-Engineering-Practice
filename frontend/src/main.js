import { createApp } from 'vue'
import {
  ElAlert, ElButton, ElCard, ElConfigProvider, ElDialog, ElEmpty,
  ElForm, ElFormItem, ElInput, ElMenu, ElMenuItem, ElRadio,
  ElRadioGroup, ElResult, ElSkeleton, ElTable, ElTableColumn, ElTag,
} from 'element-plus'
import 'element-plus/theme-chalk/base.css'
import 'element-plus/theme-chalk/el-alert.css'
import 'element-plus/theme-chalk/el-button.css'
import 'element-plus/theme-chalk/el-card.css'
import 'element-plus/theme-chalk/el-config-provider.css'
import 'element-plus/theme-chalk/el-dialog.css'
import 'element-plus/theme-chalk/el-empty.css'
import 'element-plus/theme-chalk/el-overlay.css'
import 'element-plus/theme-chalk/el-form.css'
import 'element-plus/theme-chalk/el-form-item.css'
import 'element-plus/theme-chalk/el-input.css'
import 'element-plus/theme-chalk/el-loading.css'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-menu.css'
import 'element-plus/theme-chalk/el-menu-item.css'
import 'element-plus/theme-chalk/el-radio.css'
import 'element-plus/theme-chalk/el-radio-group.css'
import 'element-plus/theme-chalk/el-result.css'
import 'element-plus/theme-chalk/el-scrollbar.css'
import 'element-plus/theme-chalk/el-skeleton.css'
import 'element-plus/theme-chalk/el-table.css'
import 'element-plus/theme-chalk/el-tag.css'
import App from './App.vue'
import router from './router'
import './styles.css'

const app = createApp(App)
app.use(router)
;[ElAlert, ElButton, ElCard, ElConfigProvider, ElDialog, ElEmpty,
  ElForm, ElFormItem, ElInput, ElMenu, ElMenuItem, ElRadio,
  ElRadioGroup, ElResult, ElSkeleton, ElTable, ElTableColumn, ElTag]
  .forEach((component) => app.use(component))
app.mount('#app')

