<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { CATEGORIES, LOCATIONS } from '../constants/catalog'
import { api } from '../api'

const router = useRouter()

const type = ref('lost')
const formRef = ref()
const form = reactive({
  title: '', category: '', location: '', time: '', desc: '',
})
const fileList = ref([])
const submitting = ref(false)

// 修复 F-04：长度上限与后端 ItemForm / schema.sql 对齐（60/500/50/20），避免"前端能输、后端 400"
const rules = {
  title: [{ required: true, message: '请填写物品名称', trigger: 'blur' },
    { max: 60, message: '标题最多 60 字', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' },
    { max: 20, message: '分类最多 20 字', trigger: 'change' }],
  location: [{ required: true, message: '请填写地点', trigger: 'blur' },
    { max: 50, message: '地点最多 50 字', trigger: 'blur' }],
  time: [{ required: true, message: '请选择时间', trigger: 'change' }],
}

const typeMeta = computed(() => type.value === 'lost'
  ? { title: '🙋 发布寻物启事', tip: '写清楚特征、上传照片，系统会自动提取感知哈希，别人「以图搜图」就能找到它', btn: '发布寻物启事' }
  : { title: '🎁 发布招领启事', tip: '描述捡到的物品并上传照片，失主通过以图搜图或关键词就能匹配到你', btn: '发布招领启事' })

function beforeUpload(file) {
  // 审查 2.27③：前端体积校验（≤5MB，与后端 multipart 上限一致），超限拦截
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('单张图片不能超过 5MB，请压缩后再上传')
    return false
  }
  return false // 阻止 el-upload 自动上传，统一在提交时用 FormData 发送
}

function onSubmit() {
  if (submitting.value) return // 审查 2.27②：防重复提交守卫
  // 修复 F-04：图片必填前端也校验（SRS FR2.1 图片 1–3 张），不发无效请求
  if (fileList.value.length < 1) {
    ElMessage.warning('请至少上传一张物品图片（以图搜图依赖图片）')
    return
  }
  formRef.value.validate(async (ok) => {
    if (!ok) return
    const fd = new FormData()
    fd.append('type', type.value)
    fd.append('title', form.title)
    fd.append('category', form.category)
    fd.append('location', form.location)
    fd.append('happenTime', form.time) // value-format 已保证 'YYYY-MM-DD HH:mm:ss'
    fd.append('description', form.desc)
    fileList.value.forEach((f) => { if (f.raw) fd.append('files', f.raw) })

    submitting.value = true
    try {
      await api.publishItem(fd)
      ElMessage.success(`${typeMeta.value.btn}成功！已提交审核，通过后即在墙上展示`)
      setTimeout(() => router.push('/profile'), 800)
    } catch (e) {
      ElMessage.error(e.message || '发布失败，请重试')
    } finally {
      submitting.value = false
    }
  })
}
</script>

<template>
  <div class="page container publish">
    <h1 class="page-title">{{ typeMeta.title }}</h1>
    <p class="page-sub">{{ typeMeta.tip }}</p>

    <!-- 类型选择大卡片 -->
    <div class="type-cards">
      <div class="t-card paper-card" :class="{ on: type === 'lost' }" @click="type = 'lost'">
        <span class="emoji">🙋</span>
        <b>我丢了东西</b>
        <small>发布寻物启事</small>
      </div>
      <div class="t-card paper-card" :class="{ on: type === 'found' }" @click="type = 'found'">
        <span class="emoji">🎁</span>
        <b>我捡到东西</b>
        <small>发布招领启事</small>
      </div>
    </div>

    <div class="form-wrap paper-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
        <el-form-item label="物品名称" prop="title">
          <el-input v-model="form.title" maxlength="60" show-word-limit placeholder="例：蓝色保温杯，杯身有小熊贴纸" />
        </el-form-item>

        <div class="row">
          <el-form-item label="分类" prop="category" class="row-item">
            <el-select v-model="form.category" placeholder="选择分类">
              <el-option v-for="c in CATEGORIES" :key="c.name" :label="`${c.emoji} ${c.name}`" :value="c.name" />
            </el-select>
          </el-form-item>
          <el-form-item label="地点" prop="location" class="row-item">
            <el-select v-model="form.location" placeholder="选择或输入地点" filterable allow-create>
              <el-option v-for="l in LOCATIONS" :key="l" :label="l" :value="l" />
            </el-select>
          </el-form-item>
          <el-form-item label="时间" prop="time" class="row-item">
            <el-date-picker v-model="form.time" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="丢失 / 捡到时间" style="width:100%" />
          </el-form-item>
        </div>

        <el-form-item label="详细描述">
          <el-input v-model="form.desc" type="textarea" :rows="4" maxlength="500" show-word-limit
            placeholder="物品特征、颜色、贴纸/划痕等细节，越详细越容易被认出（不必写联系方式，平台会转接）" />
        </el-form-item>

        <el-form-item label="物品照片（建议 1-3 张，拍清特征）">
          <el-upload v-model:file-list="fileList" list-type="picture-card" accept="image/*"
            :auto-upload="false" :before-upload="beforeUpload" :limit="3">
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <div class="demo-tip">📷 已接入真实后端：上传的图片将保存到服务器，并由 JImageHash 计算 64 位感知哈希（pHash）入库，用于以图搜图。</div>

        <el-button type="primary" size="large" round class="submit" :loading="submitting" @click="onSubmit">
          {{ submitting ? '发布中…' : typeMeta.btn }}
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.type-cards { display: flex; gap: 18px; margin-bottom: 24px; }
.t-card {
  flex: 1; max-width: 300px;
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 20px; cursor: pointer;
  transition: all .2s cubic-bezier(.34, 1.56, .64, 1);
}
.t-card .emoji { font-size: 36px; }
.t-card b { font-size: 16px; }
.t-card small { color: var(--ink-faint); font-size: 12.5px; }
.t-card:hover { transform: translateY(-4px); }
.t-card.on {
  border: 2px solid var(--orange);
  box-shadow: 0 12px 28px rgba(255, 125, 46, .28);
  transform: translateY(-4px) rotate(-1deg);
}
.t-card:last-child.on { transform: translateY(-4px) rotate(1deg); }

.form-wrap { padding: 30px 34px; max-width: 860px; }
.row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.row-item { margin-right: 0 !important; }

.submit { margin-top: 6px; padding-left: 42px; padding-right: 42px; }

@media (max-width: 720px) {
  .row { grid-template-columns: 1fr; }
  .type-cards { flex-direction: column; }
}
</style>
