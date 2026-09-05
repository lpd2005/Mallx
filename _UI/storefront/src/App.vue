<script setup>
import { inject } from 'vue'
import { useRouter } from 'vue-router'
import { ShoppingCart, User, ArrowRight } from '@element-plus/icons-vue'
import { useSession } from './services.js'
import { useTask } from '@mallx/shared/useTask.js'
const session = useSession(),
  api = inject('api'),
  router = useRouter(),
  { run } = useTask()
function logout() {
  run(async () => {
    await api.post('/auth/logout')
    session.clear()
    router.push('/')
  })
}
</script>
<template>
  <div class="store-shell">
    <div class="top-note">MallX · 让每一次选择，都恰到好处。</div>
    <header class="store-header">
      <RouterLink to="/" class="brand">
        Mall
        <span>X</span>
        <small>品质生活</small>
      </RouterLink>
      <nav>
        <RouterLink to="/">发现好物</RouterLink>
        <RouterLink to="/coupons">领券中心</RouterLink>
        <RouterLink to="/orders">我的订单</RouterLink>
      </nav>
      <div class="header-actions">
        <RouterLink to="/cart" aria-label="购物车">
          <el-icon><ShoppingCart /></el-icon>
          <span>购物车</span>
        </RouterLink>
        <RouterLink :to="session.loggedIn ? '/account' : '/login'">
          <el-icon><User /></el-icon>
          <span>{{ session.loggedIn ? '个人中心' : '登录 / 注册' }}</span>
        </RouterLink>
        <el-button v-if="session.loggedIn" link @click="logout">退出</el-button>
      </div>
    </header>
    <main class="store-main"><RouterView /></main>
    <footer class="store-footer">
      <span class="brand">
        Mall
        <span>X</span>
      </span>
      <span>好物与生活，相遇于此。</span>
      <RouterLink to="/after-sales">
        售后服务
        <el-icon><ArrowRight /></el-icon>
      </RouterLink>
    </footer>
  </div>
</template>
