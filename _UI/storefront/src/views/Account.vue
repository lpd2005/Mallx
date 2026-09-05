<script setup>
import { inject, ref, onMounted, reactive } from 'vue'
import { useTask, confirmAction } from '@mallx/shared/useTask.js'
import { dateTime } from '@mallx/shared/format.js'
import ImageUpload from '@mallx/shared/components/ImageUpload.vue'
const api = inject('api'),
  { busy, error, run } = useTask(),
  membership = ref(),
  formRef = ref(),
  ready = ref(false),
  uploading = ref(false),
  vipDialog = ref(false),
  durationMonths = ref(12)
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
function activateNormal() {
  run(async () => {
    await confirmAction('确认免费开通永久普通会员？开通后商品享95折优惠。')
    membership.value = await api.post('/users/me/membership/normal')
  }, '普通会员已开通')
}
function activateVip() {
  run(
    async () => {
      await confirmAction(
        `确认模拟${membership.value?.levelCode === 2 ? '续费' : '开通'} ${durationMonths.value} 个月VIP？本次不会发生真实扣款。`,
      )
      membership.value = await api.post('/users/me/membership/vip', {
        durationMonths: durationMonths.value,
      })
      vipDialog.value = false
    },
    membership.value?.levelCode === 2 ? 'VIP续费成功' : 'VIP开通成功',
  )
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
    <aside class="panel membership-panel" style="align-self: start">
      <div class="membership-badge">{{ membership?.levelCode === 2 ? 'VIP' : 'MX' }}</div>
      <p class="eyebrow">MEMBERSHIP</p>
      <h2>{{ membership?.levelName || '非会员' }}</h2>
      <p class="membership-benefit">
        {{
          membership?.member
            ? `商品享 ${Number(membership.discountRate) * 10} 折优惠`
            : '开通会员，结算时自动享受专属折扣'
        }}
      </p>
      <p v-if="membership?.levelCode === 1" class="muted">永久有效</p>
      <p v-if="membership?.expireAt" class="muted">
        VIP有效期至 {{ dateTime(membership.expireAt) }}
      </p>
      <el-divider />
      <div class="membership-actions">
        <el-button
          v-if="!membership?.member"
          type="primary"
          :loading="busy"
          @click="activateNormal"
        >
          免费开通普通会员
        </el-button>
        <el-button
          type="warning"
          plain
          :loading="busy"
          @click="
            () => {
              durationMonths = 12
              vipDialog = true
            }
          "
        >
          {{ membership?.levelCode === 2 ? '续费VIP' : '升级VIP' }}
        </el-button>
      </div>
      <p v-if="membership?.levelCode === 2" class="membership-note">
        VIP到期后，普通会员升级而来的账号将恢复普通会员权益。
      </p>
    </aside>
  </div>
  <el-dialog
    v-model="vipDialog"
    :title="membership?.levelCode === 2 ? '续费VIP' : '升级VIP'"
    width="500px"
    destroy-on-close
  >
    <el-alert
      title="当前为MVP模拟开通，不会产生真实扣款。"
      type="warning"
      :closable="false"
      class="error-banner"
    />
    <p class="muted">
      {{
        membership?.levelCode === 2
          ? '所选月数将从当前VIP到期时间继续累加。'
          : '开通后立即享受VIP折扣，到期时间由服务器计算。'
      }}
    </p>
    <el-form label-position="top">
      <el-form-item label="开通时长">
        <el-radio-group v-model="durationMonths" class="membership-durations">
          <el-radio-button v-for="months in [1, 3, 6, 12, 24, 36]" :key="months" :value="months">
            {{ months }}个月
          </el-radio-button>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="vipDialog = false">取消</el-button>
      <el-button type="warning" :loading="busy" @click="activateVip">
        确认模拟{{ membership?.levelCode === 2 ? '续费' : '开通' }}
      </el-button>
    </template>
  </el-dialog>
</template>
