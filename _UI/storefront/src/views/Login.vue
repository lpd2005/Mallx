<script setup>
import { useRoute, useRouter } from 'vue-router'
import LoginForm from '@mallx/shared/components/LoginForm.vue'
import { safeRedirect } from '@mallx/shared/format.js'
import { useSession } from '../services.js'
const route = useRoute(),
  router = useRouter(),
  session = useSession()
function login(auth) {
  session.save(auth)
  router.replace(safeRedirect(route.query.redirect))
}
</script>
<template>
  <div class="auth-layout">
    <div class="auth-copy">
      <p class="eyebrow" style="color: #90cfb7">WELCOME TO MALLX</p>
      <h1>
        好物相伴，
        <br />
        生活多一点期待。
      </h1>
      <p>
        收藏在心里的喜欢，
        <br />
        从这里带回日常。
      </p>
    </div>
    <LoginForm @authenticated="login" />
  </div>
</template>
