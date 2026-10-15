<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api'

/** 启事审核（审查 2.9）：待审核/已通过/已驳回 筛选 + 通过/驳回/下架/删除。 */
const tab = ref('0')   // '0' 待审核 / '1' 已通过 / '2' 已驳回 / '' 全部
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await api.adminItems({ page: page.value, size: size.value, auditStatus: tab.value === '' ? undefined : tab.value })
    list.value = res.records
    total.value = res.total
  } catch (e) {
    list.value = []   // 修复 F-11：失败时清空旧列表，避免"报错后仍显示上一次数据"的错觉
    ElMessage.error(e.message || '加载失败')
  }
  finally { loading.value = false }
}

function changeTab(t) { tab.value = t; page.value = 1; load() }

async function audit(row, approve) {
  try {
    // 修复 F-01（方案 B）：不再索要会被丢弃的"驳回理由"——改为纯确认弹窗，避免误导性交互
    // （理由落库的方案 A 涉及后端接口变更，留作后续增强）
    if (!approve) {
      await ElMessageBox.confirm(`确定驳回「${row.title}」？驳回后前台不可见，可稍后在「已驳回」页签重新通过。`,
        '驳回启事', { type: 'warning', confirmButtonText: '确定驳回', cancelButtonText: '取消' })
    }
    await api.adminAudit(row.id, approve)
    ElMessage.success(approve ? '已通过审核' : '已驳回')
    load()
  } catch (e) { if (e !== 'cancel') ElMessage.error(e.message || '操作失败') }
}

async function offline(row) {
  // 修复 F-05：区分"用户取消"与"接口失败"，失败必须提示
  try { await ElMessageBox.confirm(`确定下架「${row.title}」？`, '违规下架', { type: 'warning' }) }
  catch { return }
  try { await api.adminOffline(row.id); ElMessage.success('已下架'); load() }
  catch (e) { ElMessage.error(e.message || '下架失败') }
}

async function del(row) {
  try { await ElMessageBox.confirm(`删除「${row.title}」将同时清理图片与认领记录，不可恢复。继续？`, '删除启事', { type: 'error' }) }
  catch { return }
  try { await api.adminDelete(row.id); ElMessage.success('已删除'); load() }
  catch (e) { ElMessage.error(e.message || '删除失败') }
}

onMounted(load)
</script>

<template>
  <div class="paper-card wrap" v-loading="loading">
    <div class="tabs">
      <button :class="{ on: tab === '0' }" @click="changeTab('0')">待审核</button>
      <button :class="{ on: tab === '1' }" @click="changeTab('1')">已通过</button>
      <button :class="{ on: tab === '2' }" @click="changeTab('2')">已驳回</button>
      <button :class="{ on: tab === '' }" @click="changeTab('')">全部</button>
    </div>

    <el-table :data="list" style="width: 100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="封面" width="80">
        <template #default="{ row }"><img class="cov" :src="row.thumb || row.img" alt="" /></template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="70">
        <template #default="{ row }">{{ row.type === 'lost' ? '寻物' : '招领' }}</template>
      </el-table-column>
      <el-table-column prop="owner" label="发布人" width="90" />
      <el-table-column prop="status" label="状态" width="90" />
      <el-table-column label="审核" width="90">
        <template #default="{ row }">
          <el-tag :type="row.auditStatus === 1 ? 'success' : row.auditStatus === 0 ? 'warning' : 'danger'" size="small" round>
            {{ row.auditStatus === 1 ? '已通过' : row.auditStatus === 0 ? '待审核' : '已驳回' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.auditStatus === 0" size="small" type="primary" @click="audit(row, true)">通过</el-button>
          <el-button v-if="row.auditStatus === 0" size="small" @click="audit(row, false)">驳回</el-button>
          <!-- 修复 F-01/B-03 联动：已驳回启事可"重新通过"（后端 B-03 已支持恢复可见） -->
          <el-button v-if="row.auditStatus === 2" size="small" type="success" plain @click="audit(row, true)">重新通过</el-button>
          <el-button v-if="row.auditStatus === 1" size="small" type="warning" plain @click="offline(row)">下架</el-button>
          <el-button size="small" type="danger" plain @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" layout="prev, pager, next, total" :total="total" :page-size="size"
      v-model:current-page="page" @current-change="load" />
  </div>
</template>

<style scoped>
.wrap { padding: 18px; }
.tabs { display: flex; gap: 8px; margin-bottom: 14px; }
.tabs button {
  padding: 7px 18px; border-radius: 999px; border: 1.5px solid var(--line);
  background: var(--paper); color: var(--ink-soft); cursor: pointer; font-size: 13.5px;
}
.tabs button.on { background: var(--orange); border-color: transparent; color: #FFF7EC; font-weight: 700; }
.cov { width: 56px; height: 42px; object-fit: cover; border-radius: 8px; background: var(--cream-deep); }
.pager { margin-top: 14px; justify-content: center; }
</style>
