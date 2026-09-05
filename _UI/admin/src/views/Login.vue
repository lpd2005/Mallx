<script setup>
import { inject } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import LoginForm from '@mallx/shared/components/LoginForm.vue'
import { useTask } from '@mallx/shared/useTask.js'
import { safeRedirect } from '@mallx/shared/format.js'
import { useSession } from '../services.js'
const api = inject('api'),
  session = useSession(),
  route = useRoute(),
  router = useRouter(),
  { busy, error, run } = useTask()
function login(auth) {
  run(async () => {
    session.save(auth)
    try {
      session.admin = await api.get('/admin/me')
      router.replace(safeRedirect(route.query.redirect))
    } catch (e) {
      session.clear()
      throw e
    }
  })
}
</script>
<template>
  <div class="admin-login">
    <div class="admin-login-intro">
      <div class="admin-brand">
        Mall
        <span>X</span>
        <small>运营管理平台</small>
      </div>
      <h1>
        经营有序，
        <br />
        每一步心中有数。
      </h1>
      <p>商品 · 库存 · 交易 · 服务</p>
    </div>
    <div class="admin-login-form" v-loading="busy">
      <el-alert v-if="error" :title="error" type="error" :closable="false" class="error-banner" />
      <LoginForm admin @authenticated="login" />
    </div>
  </div>
</template>
