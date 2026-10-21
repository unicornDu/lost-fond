<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { CATEGORIES, LOCATIONS } from '../constants/catalog'
import { api } from '../api'
import { catLabel, t } from '../i18n'
import LocationPicker from '../components/LocationPicker.vue'

const router = useRouter()

const type = ref('lost')
const formRef = ref()
const form = reactive({
  title: '', category: '', location: '', time: '', desc: '',
})
const coords = ref(null)   // v2.41.1：地图选点（可空）
const fileList = ref([])
const submitting = ref(false)

// 修复 F-04：长度上限与后端 ItemForm / schema.sql 对齐（60/500/50/20），避免"前端能输、后端 400"
// computed：切语言时校验文案同步更新
const rules = computed(() => ({
  title: [{ required: true, message: t('valid.titleRequired'), trigger: 'blur' },
    { max: 60, message: t('valid.titleMax'), trigger: 'blur' }],
  category: [{ required: true, message: t('valid.categoryRequired'), trigger: 'change' },
    { max: 20, message: t('valid.categoryMax'), trigger: 'change' }],
  location: [{ required: true, message: t('valid.locationRequired'), trigger: 'blur' },
    { max: 50, message: t('valid.locationMax'), trigger: 'blur' }],
  time: [{ required: true, message: t('valid.timeRequired'), trigger: 'change' }],
}))

const typeMeta = computed(() => type.value === 'lost'
  ? { title: t('publish.lost.title'), tip: t('publish.lost.tip'), btn: t('publish.lost.btn') }
  : { title: t('publish.found.title'), tip: t('publish.found.tip'), btn: t('publish.found.btn') })

function beforeUpload(file) {
  // 审查 2.27③：前端体积校验（≤5MB，与后端 multipart 上限一致），超限拦截
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error(t('publish.size'))
    return false
  }
  return false // 阻止 el-upload 自动上传，统一在提交时用 FormData 发送
}

function onSubmit() {
  if (submitting.value) return // 审查 2.27②：防重复提交守卫
  // 修复 F-04：图片必填前端也校验（SRS FR2.1 图片 1–3 张），不发无效请求
  if (fileList.value.length < 1) {
    ElMessage.warning(t('publish.needPhoto'))
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
    // v2.41.1：可选精确位置（GCJ-02）；不标就不传，后端存 NULL
    if (coords.value) {
      fd.append('latitude', Number(coords.value.lat).toFixed(6))
      fd.append('longitude', Number(coords.value.lng).toFixed(6))
    }
    fileList.value.forEach((f) => { if (f.raw) fd.append('files', f.raw) })

    submitting.value = true
    try {
      await api.publishItem(fd)
      ElMessage.success(t('publish.ok', { btn: typeMeta.value.btn }))
      window.dispatchEvent(new Event('shiguang-items-changed'))
      setTimeout(() => router.push('/profile'), 800)
    } catch (e) {
      ElMessage.error(e.message || t('publish.fail'))
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
        <b>{{ $t('publish.type.lost') }}</b>
        <small>{{ $t('publish.type.lostSub') }}</small>
      </div>
      <div class="t-card paper-card" :class="{ on: type === 'found' }" @click="type = 'found'">
        <span class="emoji">🎁</span>
        <b>{{ $t('publish.type.found') }}</b>
        <small>{{ $t('publish.type.foundSub') }}</small>
      </div>
    </div>

    <div class="form-wrap paper-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
        <el-form-item :label="$t('publish.form.title')" prop="title">
          <el-input v-model="form.title" maxlength="60" show-word-limit :placeholder="$t('publish.form.titlePh')" />
        </el-form-item>

        <div class="row">
          <el-form-item :label="$t('publish.form.category')" prop="category" class="row-item">
            <el-select v-model="form.category" :placeholder="$t('publish.form.categoryPh')">
              <el-option v-for="c in CATEGORIES" :key="c.name" :label="`${c.emoji} ${catLabel(c.name)}`" :value="c.name" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('publish.form.location')" prop="location" class="row-item">
            <el-select v-model="form.location" :placeholder="$t('publish.form.locationPh')" filterable allow-create>
              <el-option v-for="l in LOCATIONS" :key="l" :label="l" :value="l" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('publish.form.time')" prop="time" class="row-item">
            <el-date-picker v-model="form.time" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
              :placeholder="$t('publish.form.timePh')" style="width:100%" />
          </el-form-item>
        </div>

        <el-form-item :label="$t('publish.form.map')" class="map-field">
          <LocationPicker v-model="coords" />
          <p class="map-tip">{{ $t('publish.form.mapTip') }}</p>
        </el-form-item>

        <el-form-item :label="$t('publish.form.desc')">
          <el-input v-model="form.desc" type="textarea" :rows="4" maxlength="500" show-word-limit
            :placeholder="$t('publish.form.descPh')" />
        </el-form-item>

        <el-form-item :label="$t('publish.form.photos')">
          <el-upload v-model:file-list="fileList" list-type="picture-card" accept="image/*"
            :auto-upload="false" :before-upload="beforeUpload" :limit="3">
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <div class="demo-tip"><span>{{ $t('publish.tip') }}</span></div>

        <el-button type="primary" size="large" round class="submit" :loading="submitting" @click="onSubmit">
          {{ submitting ? $t('publish.submitting') : typeMeta.btn }}
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.map-field { display: block; }
.map-tip { margin: 8px 0 0; font-size: 11.5px; line-height: 1.6; color: var(--ink-faint); }
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
  box-shadow: 0 12px 28px rgba(20, 20, 18, .16);
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
