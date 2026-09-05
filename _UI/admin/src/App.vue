<script setup>
import { computed, ref, inject } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as icons from '@element-plus/icons-vue'
import { navigation } from './navigation.js'
import { useSession } from './services.js'
import { useTask } from '@mallx/shared/useTask.js'
const session = useSession(),
  route = useRoute(),
  router = useRouter(),
  api = inject('api'),
  { run } = useTask(),
  open = ref(false)
const groups = computed(() => {
  const entries = navigation.filter((n) => session.can(n.permission))
  return [...new Set(entries.map((n) => n.group))].map((title) => ({
    title,
    items: entries.filter((n) => n.group === title),
  }))
})
function logout() {
  run(async () => {
    await api.post('/auth/logout')
    session.clear()
    router.replace('/login')
  })
}
</script>
<template>
  <RouterView v-if="route.path === '/login'" />
  <div v-else class="admin-shell">
    <aside class="admin-sidebar" :class="{ open }">
      <RouterLink to="/" class="admin-brand">
        Mall
        <span>X</span>
        <small>运营管理平台</small>
      </RouterLink>
      <nav>
        <section v-for="g in groups" :key="g.title">
          <p>{{ g.title }}</p>
          <RouterLink v-for="n in g.items" :key="n.path" :to="n.path" @click="open = false">
            <el-icon><component :is="icons[n.icon]" /></el-icon>
            {{ n.title }}
          </RouterLink>
        </section>
      </nav>
      <div class="sidebar-foot">
        <span class="status-dot" />
        MallX 工作空间
      </div>
    </aside>
    <div class="admin-body">
      <header class="admin-header">
        <div class="actions">
          <el-button
            class="menu-toggle"
            :icon="icons.Menu"
            aria-label="展开菜单"
            @click="open = !open"
          />
          <span class="muted">工作空间 /</span>
          <b>{{ route.meta.title }}</b>
        </div>
        <div class="actions">
          <el-tag type="info">{{ session.admin?.roles?.join(' / ') || '管理员' }}</el-tag>
          <el-button link @click="logout">退出登录</el-button>
        </div>
      </header>
      <main class="admin-main"><RouterView :key="route.path" /></main>
    </div>
  </div>
</template>
