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
  } catch (e) { ElMessage.error(e.message || '加载失败') }
  finally { loading.value = false }
}

function changeTab(t) { tab.value = t; page.value = 1; load() }

async function audit(row, approve) {
  try {
    if (!approve) {
      const r = await ElMessageBox.prompt('驳回理由（选填）', '驳回启事', { confirmButtonText: '确定驳回', cancelButtonText: '取消' })
      await api.adminAudit(row.id, false); void r
    } else {
      await api.adminAudit(row.id, true)
    }
    ElMessage.success(approve ? '已通过审核' : '已驳回')
    load()
  } catch (e) { if (e !== 'cancel') ElMessage.error(e.message || '操作失败') }
}

async function offline(row) {
  await ElMessageBox.confirm(`确定下架「${row.title}」？`, '违规下架', { type: 'warning' })
    .then(async () => { await api.adminOffline(row.id); ElMessage.success('已下架'); load() })
    .catch(() => {})
}

async function del(row) {
  await ElMessageBox.confirm(`删除「${row.title}」将同时清理图片与认领记录，不可恢复。继续？`, '删除启事', { type: 'error' })
    .then(async () => { await api.adminDelete(row.id); ElMessage.success('已删除'); load() })
    .catch(() => {})
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
