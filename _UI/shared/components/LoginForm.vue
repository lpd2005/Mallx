<script setup>
import { ref, reactive, inject } from 'vue'
import { ElMessage } from 'element-plus'
import { useTask } from '../useTask.js'
defineProps({ admin: Boolean })
const emit = defineEmits(['authenticated'])
const api = inject('api'),
  { busy, error, run } = useTask(),
  register = ref(false),
  formRef = ref()
const form = reactive({ phone: '', password: '', nickname: '', confirm: '' })
const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的大陆手机号', trigger: 'blur' },
  ],
  password: [
    {
      validator: (_, value, done) => {
        if (!value) return done(new Error('请输入密码'))
        if (
          register.value &&
          (value.length < 8 || value.length > 64 || new TextEncoder().encode(value).length > 72)
        )
          return done(new Error('密码需8～64位，UTF-8编码不超过72字节'))
        done()
      },
      trigger: 'blur',
    },
  ],
  confirm: [
    {
      validator: (_, value, done) => {
        if (register.value && value !== form.password) return done(new Error('两次密码不一致'))
        done()
      },
      trigger: 'blur',
    },
  ],
}
async function submit() {
  if (!(await formRef.value.validate().catch(() => false))) return
  run(async () => {
    if (register.value) {
      await api.post('/auth/register', {
        phone: form.phone,
        password: form.password,
        nickname: form.nickname || undefined,
      })
      ElMessage.success('注册成功，请登录')
      register.value = false
      form.confirm = ''
      return
    }
    const auth = await api.post('/auth/login', { phone: form.phone, password: form.password })
    emit('authenticated', auth)
  })
}
</script>
<template>
  <div class="auth-form">
    <h2>{{ register ? '创建 MallX 账户' : admin ? '登录管理平台' : '欢迎回来' }}</h2>
    <p class="muted">
      {{
        register
          ? '使用手机号和密码注册'
          : admin
            ? '使用已获授权的管理员账号'
            : '登录后继续你的购物旅程'
      }}
    </p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" class="error-banner" />
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      @submit.prevent="submit"
    >
      <el-form-item label="手机号" prop="phone">
        <el-input
          v-model="form.phone"
          inputmode="tel"
          maxlength="11"
          autocomplete="username"
          size="large"
        />
      </el-form-item>
      <el-form-item v-if="register" label="昵称（选填）">
        <el-input v-model="form.nickname" maxlength="32" size="large" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          :maxlength="register ? 64 : 128"
          :autocomplete="register ? 'new-password' : 'current-password'"
          size="large"
        />
      </el-form-item>
      <el-form-item v-if="register" label="确认密码" prop="confirm">
        <el-input
          v-model="form.confirm"
          type="password"
          show-password
          autocomplete="new-password"
          size="large"
        />
      </el-form-item>
      <el-button native-type="submit" type="primary" size="large" :loading="busy">
        {{ register ? '注册' : '登录' }}
      </el-button>
    </el-form>
    <el-button
      v-if="!admin"
      link
      class="section-gap"
      @click="
        () => {
          register = !register
          formRef?.clearValidate()
        }
      "
    >
      {{ register ? '已有账号？返回登录' : '还没有账号？立即注册' }}
    </el-button>
  </div>
</template>
