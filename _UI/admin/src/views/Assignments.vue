<script setup>
import { inject, ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { allPages, useSession } from '../services.js'
import { useTask, confirmAction } from '@mallx/shared/useTask.js'
const api = inject('api'),
  route = useRoute(),
  session = useSession(),
  { busy, error, run } = useTask(),
  userId = ref(String(route.query.userId || '')),
  loadedId = ref(''),
  roles = ref([]),
  selected = ref([]),
  original = ref([]),
  rolePermissions = ref({})
const own = computed(() => loadedId.value === String(session.admin?.userId)),
  protectedUser = computed(() =>
    roles.value.some((r) => r.roleCode === 'SYSTEM_ADMIN' && original.value.includes(r.id)),
  )
async function load() {
  if (!/^[1-9]\d*$/.test(userId.value.trim())) throw new Error('请输入有效的用户ID')
  const id = userId.value.trim()
  const [all, ids] = await Promise.all([
    allPages('/admin/rbac/roles'),
    api.get(`/admin/rbac/users/${id}/roles`),
  ])
  const directory = await api.get('/admin/rbac/permissions')
  const codes = new Map(directory.map((p) => [String(p.id), p.permissionCode]))
  const mapping = {}
  await Promise.all(
    all
      .filter((r) => r.roleCode !== 'SYSTEM_ADMIN')
      .map(async (r) => {
        mapping[r.id] = (await api.get(`/admin/rbac/roles/${r.id}/permissions`)).map((p) =>
          codes.get(String(p)),
        )
      }),
  )
  roles.value = all
  rolePermissions.value = mapping
  selected.value = [...ids]
  original.value = [...ids]
  loadedId.value = id
}
function allowed(role) {
  return (
    role.roleCode !== 'SYSTEM_ADMIN' &&
    (rolePermissions.value[role.id] || []).every((code) => session.can(code))
  )
}
function save() {
  run(async () => {
    await confirmAction(
      `确认替换账号 ${loadedId.value} 的角色？${selected.value.length ? '' : '当前选择将清空其全部角色。'}`,
    )
    await api.put(`/admin/rbac/users/${loadedId.value}/roles`, { ids: selected.value })
    await load()
  }, '账号角色已更新')
}
onMounted(() => {
  if (userId.value) run(load)
})
</script>
<template>
  <div class="page-head"><h1>账号角色分配</h1></div>
  <el-alert v-if="error" :title="error" type="error" :closable="false" class="error-banner" />
  <section class="panel">
    <div class="toolbar">
      <el-input
        v-model="userId"
        placeholder="输入用户ID"
        style="width: 340px"
        @keyup.enter="run(load)"
      />
      <el-button type="primary" :loading="busy" @click="run(load)">查询当前角色</el-button>
    </div>
    <p class="muted">用户ID可在用户管理列表查看。</p>
    <template v-if="loadedId">
      <el-divider />
      <h2>账号 {{ loadedId }}</h2>
      <el-alert
        v-if="own || protectedUser"
        :title="own ? '不能修改自己的账号角色' : '系统管理员账号的角色受到保护'"
        type="warning"
        :closable="false"
        class="error-banner"
      />
      <el-checkbox-group
        v-model="selected"
        class="permission-grid"
        :disabled="own || protectedUser || busy"
      >
        <el-checkbox
          v-for="r in roles"
          :key="r.id"
          :value="r.id"
          :disabled="r.roleCode === 'SYSTEM_ADMIN' || (!allowed(r) && !selected.includes(r.id))"
        >
          {{ r.roleName }}
          <span class="muted">{{ r.roleCode }} · {{ r.status === 1 ? '启用' : '禁用' }}</span>
        </el-checkbox>
      </el-checkbox-group>
      <el-button
        type="primary"
        class="section-gap"
        :loading="busy"
        :disabled="own || protectedUser || loadedId !== userId.trim()"
        @click="save"
      >
        保存角色分配
      </el-button>
    </template>
  </section>
</template>
