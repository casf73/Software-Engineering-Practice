<template>
 <AppCard><template #header><h2>两级分类管理</h2></template>
  <el-form label-position="top" @submit.prevent="save">
   <el-form-item label="分类名称"><el-input v-model="form.name" maxlength="80" /></el-form-item>
   <el-form-item label="父分类"><select v-model="form.parentId" class="native-select"><option value="">无（一级分类）</option><option v-for="c in roots" :value="c.id" :key="c.id">{{ c.name }}</option></select></el-form-item>
   <el-button type="primary" native-type="submit" :loading="busy">{{ editingId ? '保存修改' : '新增分类' }}</el-button><el-button @click="reset">取消编辑</el-button>
  </el-form>
  <el-table :data="rows" stripe><el-table-column prop="id" label="编号" width="80"/><el-table-column prop="name" label="分类名称"/><el-table-column label="层级"><template #default="s">{{ s.row.parentId ? '二级' : '一级' }}</template></el-table-column><el-table-column label="父分类"><template #default="s">{{ rows.find(x => x.id === s.row.parentId)?.name || '—' }}</template></el-table-column><el-table-column label="操作"><template #default="s"><el-button link @click="edit(s.row)">编辑</el-button><el-button link type="danger" @click="remove(s.row)">删除</el-button></template></el-table-column></el-table>
 </AppCard>
 <AppDialog v-model="deleteVisible" title="删除分类"><p>确认删除“{{ deleting?.name }}”？包含商品或子分类的分类不能删除。</p><template #footer><el-button @click="deleteVisible = false">取消</el-button><el-button type="danger" :loading="busy" @click="confirmDelete">删除</el-button></template></AppDialog>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AppCard from './common/AppCard.vue'
import AppDialog from './common/AppDialog.vue'
import { sellerApi } from '../api/sellerApi'
import { apiErrorMessage } from '../api/http'
const emit = defineEmits(['changed'])
const rows = ref([]), editingId = ref(null), busy = ref(false), deleting = ref(null), deleteVisible = ref(false)
const form = reactive({ name: '', parentId: '' })
const roots = computed(() => rows.value.filter(c => !c.parentId && c.id !== editingId.value))
async function load() { try { rows.value = (await sellerApi.categories()).data } catch(e) { ElMessage.error(apiErrorMessage(e)) } }
function reset() { editingId.value = null; form.name = ''; form.parentId = '' }
function edit(c) { editingId.value = c.id; form.name = c.name; form.parentId = c.parentId || '' }
async function save() { if(busy.value)return; if(!form.name.trim()){ElMessage.warning('请输入分类名称');return} busy.value = true; try { await sellerApi.saveCategory(editingId.value, { name: form.name.trim(), parentId: form.parentId || null }); reset(); await load(); emit('changed'); ElMessage.success('分类已保存') } catch(e) { ElMessage.error(apiErrorMessage(e)) } finally { busy.value = false } }
function remove(c) { deleting.value = c; deleteVisible.value = true }
async function confirmDelete() { if(busy.value)return; busy.value = true; try { await sellerApi.deleteCategory(deleting.value.id); deleteVisible.value = false; reset(); await load(); emit('changed') } catch(e) { ElMessage.error(apiErrorMessage(e)) } finally { busy.value = false } }
onMounted(load)
</script>
