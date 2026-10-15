<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api'

/** 操作日志（审查 2.29③）：分页读取 admin_log。 */
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(15)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await api.adminLogs({ page: page.value, size: size.value })
    list.value = res.records
    total.value = res.total
  } catch (e) { ElMessage.error(e.message || '加载失败') }
  finally { loading.value = false }
}

const actionLabel = {
  audit: '审核', offline: '下架', delete: '删除', ban: '封禁', unban: '解封', 'regen-thumbs': '补缩略图',
}

onMounted(load)
</script>

<template>
  <div class="paper-card wrap" v-loading="loading">
    <el-table :data="list" style="width: 100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="adminId" label="管理员ID" width="90" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-tag size="small" round :type="row.action === 'delete' ? 'danger' : row.action === 'ban' ? 'warning' : 'success'">
            {{ actionLabel[row.action] || row.action }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="targetType" label="对象类型" width="90" />
      <el-table-column prop="targetId" label="对象ID" width="80" />
      <el-table-column prop="detail" label="详情" min-width="200" show-overflow-tooltip />
      <el-table-column prop="createTime" label="时间" width="170" />
    </el-table>
    <el-pagination class="pager" layout="prev, pager, next, total" :total="total" :page-size="size"
      v-model:current-page="page" @current-change="load" />
  </div>
</template>

<style scoped>
.wrap { padding: 18px; }
.pager { margin-top: 14px; justify-content: center; }
</style>
