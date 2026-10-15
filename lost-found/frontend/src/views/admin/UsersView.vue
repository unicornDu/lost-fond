<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api'

/** 用户管理（审查 2.9）：搜索 + 封禁/解封（按 status 切换文案与颜色）。 */
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await api.adminUsers({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    list.value = res.records
    total.value = res.total
  } catch (e) { ElMessage.error(e.message || '加载失败') }
  finally { loading.value = false }
}

async function toggleBan(row) {
  const ban = row.status === 1
  await ElMessageBox.confirm(`${ban ? '封禁' : '解封'}用户「${row.nickname}」？`, '确认', { type: 'warning' })
    .then(async () => {
      await api.adminSetUserStatus(row.id, ban)
      ElMessage.success(ban ? '已封禁' : '已解封')
      load()
    }).catch(() => {})
}

onMounted(load)
</script>

<template>
  <div class="paper-card wrap" v-loading="loading">
    <div class="bar">
      <el-input v-model="keyword" placeholder="搜索学号 / 昵称" clearable class="kw" @keyup.enter="page = 1; load()" @clear="page = 1; load()" />
      <el-button type="primary" round @click="page = 1; load()">搜索</el-button>
    </div>
    <el-table :data="list" style="width: 100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="account" label="学号" width="120" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column prop="avatar" label="头像" width="60" />
      <el-table-column prop="createTime" label="注册时间" width="140" />
      <el-table-column label="角色" width="90">
        <template #default="{ row }">
          <el-tag :type="row.role === 1 ? 'danger' : 'info'" size="small" round>{{ row.role === 1 ? '管理员' : '用户' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" round>{{ row.status === 1 ? '正常' : '封禁' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.role !== 1" size="small" :type="row.status === 1 ? 'danger' : 'success'" plain @click="toggleBan(row)">
            {{ row.status === 1 ? '封禁' : '解封' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" layout="prev, pager, next, total" :total="total" :page-size="size"
      v-model:current-page="page" @current-change="load" />
  </div>
</template>

<style scoped>
.wrap { padding: 18px; }
.bar { display: flex; gap: 10px; margin-bottom: 14px; }
.bar .kw { max-width: 280px; }
.pager { margin-top: 14px; justify-content: center; }
</style>
