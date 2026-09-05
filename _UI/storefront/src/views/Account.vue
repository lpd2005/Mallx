<script setup>
import { inject, ref, onMounted, reactive } from 'vue'
import { useTask } from '@mallx/shared/useTask.js'
import { dateTime } from '@mallx/shared/format.js'
import ImageUpload from '@mallx/shared/components/ImageUpload.vue'
const api = inject('api'),
  { busy, error, run } = useTask(),
  membership = ref(),
  formRef = ref(),
  ready = ref(false),
  uploading = ref(false)
const form = reactive({ nickname: '', avatarOssKey: '', gender: 0, birthday: null, bio: '' })
async function load() {
  const [profile, member] = await Promise.all([
    api.get('/users/me'),
    api.get('/users/me/membership'),
  ])
  for (const k of Object.keys(form)) form[k] = profile[k]
  membership.value = member
  ready.value = true
}
async function save() {
  if (uploading.value) return
  if (!(await formRef.value.validate().catch(() => false))) return
  run(() => api.put('/users/me', { ...form }), '个人资料已保存')
}
onMounted(() => run(load))
</script>
<template>
  <div class="page-head"><h1>个人中心</h1></div>
  <nav class="account-nav">
    <RouterLink to="/account">个人资料</RouterLink>
    <RouterLink to="/addresses">收货地址</RouterLink>
    <RouterLink to="/coupons?tab=mine">我的优惠券</RouterLink>
    <RouterLink to="/after-sales">售后服务</RouterLink>
  </nav>
  <el-alert v-if="error" :title="error" type="error" class="error-banner" :closable="false">
    <el-button link @click="run(load)">重新加载</el-button>
  </el-alert>
  <div class="two-cols" v-loading="busy">
    <section class="panel">
      <el-form
        ref="formRef"
        :model="form"
        label-position="top"
        class="account-form"
        @submit.prevent="save"
      >
        <el-form-item label="头像">
          <ImageUpload v-model="form.avatarOssKey" avatar @uploading="uploading = $event" />
        </el-form-item>
        <el-form-item
          label="昵称"
          prop="nickname"
          :rules="[{ required: true, message: '请填写昵称' }]"
        >
          <el-input v-model="form.nickname" maxlength="32" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="性别">
            <el-select v-model="form.gender">
              <el-option :value="0" label="保密" />
              <el-option :value="1" label="男" />
              <el-option :value="2" label="女" />
            </el-select>
          </el-form-item>
          <el-form-item label="生日">
            <el-date-picker
              v-model="form.birthday"
              value-format="YYYY-MM-DD"
              style="width: 100%"
              :disabled-date="(d) => d > Date.now()"
            />
          </el-form-item>
        </div>
        <el-form-item label="个人简介">
          <el-input v-model="form.bio" type="textarea" maxlength="255" show-word-limit />
        </el-form-item>
        <el-button
          type="primary"
          native-type="submit"
          :loading="busy"
          :disabled="!ready || uploading"
        >
          保存资料
        </el-button>
      </el-form>
    </section>
    <aside class="panel" style="align-self: start">
      <p class="eyebrow">MEMBERSHIP</p>
      <h2>{{ membership?.levelName || '非会员' }}</h2>
      <p>
        {{
          membership?.member
            ? `商品享 ${Number(membership.discountRate) * 10} 折优惠`
            : '当前按商品原价结算'
        }}
      </p>
      <p v-if="membership?.expireAt" class="muted">到期时间 {{ dateTime(membership.expireAt) }}</p>
    </aside>
  </div>
</template>
