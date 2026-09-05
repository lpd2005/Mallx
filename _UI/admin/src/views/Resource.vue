<script setup>
import { inject, ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { resources } from '../resources.js'
import { useSession } from '../services.js'
import { useTask, confirmAction } from '@mallx/shared/useTask.js'
import { money, dateTime } from '@mallx/shared/format.js'
import ResourceForm from '../components/ResourceForm.vue'
import Picture from '@mallx/shared/components/Picture.vue'
import StatusTag from '@mallx/shared/components/StatusTag.vue'
const api = inject('api'),
  route = useRoute(),
  router = useRouter(),
  session = useSession(),
  { busy, error, run } = useTask(),
  config = resources[route.meta.resource]
const rows = ref([]),
  total = ref(0),
  query = reactive({
    current: 1,
    size: 20,
    keyword: '',
    status: undefined,
    extra: String(route.query.spuId || ''),
  }),
  visible = ref(false),
  editId = ref(),
  form = reactive({}),
  formRef = ref(),
  permissionDialog = ref(false),
  permissions = ref([]),
  permissionIds = ref([]),
  role = ref(),
  profile = ref(),
  profileDialog = ref(false)
const columns = computed(() => config.columns)
async function load() {
  const params = {
    current: query.current,
    size: query.size,
    status: query.status,
    keyword: query.keyword.trim() || undefined,
  }
  if (config.extraFilter) params[config.extraFilter.key] = query.extra.trim() || undefined
  const response = await api.get(config.endpoint, params)
  rows.value = config.list ? response : response.records
  total.value = config.list ? response.length : Number(response.total)
}
function search() {
  query.current = 1
  run(load)
}
function edit(row) {
  Object.keys(form).forEach((k) => delete form[k])
  for (const f of config.fields)
    form[f.key] =
      row?.[f.key] ??
      f.default ??
      (f.type === 'number' || f.type === 'lookup' || f.type === 'datetime' ? undefined : '')
  editId.value = row?.id
  visible.value = true
}
async function save() {
  if (formRef.value?.uploading) return
  if (!(await formRef.value.validate().catch(() => false))) return
  run(async () => {
    const body = Object.fromEntries(
      config.fields.filter((f) => !f.when || f.when(form)).map((f) => [f.key, form[f.key] ?? null]),
    )
    if (config.shelf && body.marketPrice != null && body.marketPrice < body.salePrice)
      throw new Error('划线价不能低于销售价')
    if (body.specJson) {
      const value = JSON.parse(body.specJson)
      if (!value || Array.isArray(value) || typeof value !== 'object')
        throw new Error('规格必须为有效对象')
    }
    if (config.publish) {
      if (
        !(body.claimStartAt < body.claimEndAt) ||
        !(body.validStartAt < body.validEndAt) ||
        !(body.validEndAt > body.claimEndAt)
      )
        throw new Error('请检查时间范围，使用截止时间须晚于领取截止时间')
    }
    if (editId.value) await api.put(`${config.endpoint}/${editId.value}`, body)
    else await api.post(config.endpoint, body)
    visible.value = false
    await load()
  }, '保存成功')
}
function remove(row) {
  run(async () => {
    await confirmAction('确认删除此记录？有关联数据时后端会拒绝删除。')
    await api.delete(`${config.endpoint}/${row.id}`)
    await load()
  }, '删除成功')
}
function shelf(row) {
  const status = row.status === 1 ? 2 : 1
  run(async () => {
    await confirmAction(`确认${status === 1 ? '上架' : '下架'}该商品？`)
    await api.put(`${config.endpoint}/${row.id}/status`, { status })
    await load()
  }, '状态已更新')
}
function publish(row, action) {
  run(async () => {
    await confirmAction(
      action === 'publish' ? '确认发布此优惠券？发布后不可编辑。' : '确认停止发放此优惠券？',
    )
    await api.post(`${config.endpoint}/${row.id}/${action}`)
    await load()
  }, '状态已更新')
}
function userStatus(row) {
  run(async () => {
    await confirmAction(
      row.status === 1 ? '禁用后该用户将无法访问需登录的业务。确认禁用？' : '确认启用此用户？',
    )
    await api.put(`/admin/users/${row.id}/status`, { status: row.status === 1 ? 0 : 1 })
    await load()
  }, '账号状态已更新')
}
function showProfile(row) {
  run(async () => {
    profile.value = await api.get(`/admin/users/${row.id}/profile`)
    profileDialog.value = true
  })
}
function editPermissions(row) {
  run(async () => {
    role.value = row
    ;[permissions.value, permissionIds.value] = await Promise.all([
      api.get('/admin/rbac/permissions'),
      api.get(`/admin/rbac/roles/${row.id}/permissions`),
    ])
    permissionDialog.value = true
  })
}
function savePermissions() {
  run(async () => {
    await confirmAction(`确认更新「${role.value.roleName}」的权限？将替换其现有权限。`)
    await api.put(`/admin/rbac/roles/${role.value.id}/permissions`, { ids: permissionIds.value })
    permissionDialog.value = false
  }, '权限已更新')
}
const editable = (row) => (!config.publish || row.status === 0) && row.roleCode !== 'SYSTEM_ADMIN'
onMounted(() => run(load))
</script>
<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">MALLX ADMIN</p>
      <h1>{{ config.title }}</h1>
    </div>
    <el-button v-if="config.create" type="primary" @click="edit()">
      新增{{ config.role ? '角色' : config.publish ? '优惠券' : config.title.replace('管理', '') }}
    </el-button>
  </div>
  <el-alert v-if="error" :title="error" type="error" :closable="false" class="error-banner" />
  <section class="panel">
    <div class="toolbar">
      <el-input
        v-if="!config.noKeyword"
        v-model="query.keyword"
        :placeholder="config.keywordLabel || '输入名称或编码查找'"
        maxlength="128"
        clearable
        @keyup.enter="search"
        @clear="search"
      />
      <el-input
        v-if="config.extraFilter"
        v-model="query.extra"
        :placeholder="config.extraFilter.label"
        class="filter-id"
        clearable
        @keyup.enter="search"
      />
      <el-select
        v-if="config.states"
        v-model="query.status"
        placeholder="全部状态"
        clearable
        @change="search"
      >
        <el-option
          v-for="(label, value) in config.states"
          :key="value"
          :value="Number(value)"
          :label="label"
        />
      </el-select>
      <el-button type="primary" :loading="busy" @click="search">查询</el-button>
    </div>
    <el-table :data="rows" v-loading="busy" row-key="id">
      <el-table-column
        v-for="c in columns"
        :key="c.key"
        :label="c.label"
        :width="c.width"
        :min-width="c.width ? undefined : 160"
      >
        <template #default="{ row }">
          <Picture v-if="c.type === 'image'" :src="row[c.key]" class="resource-image" />
          <StatusTag v-else-if="c.states" :value="row[c.key]" :states="c.states" />
          <span v-else-if="c.type === 'money'">{{ money(row[c.key]) }}</span>
          <span v-else-if="c.type === 'date'">{{ dateTime(row[c.key]) }}</span>
          <span v-else>{{ row[c.key] ?? '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column
        v-if="!config.readonly"
        label="操作"
        fixed="right"
        :width="config.user ? 250 : config.shelf ? 220 : 190"
      >
        <template #default="{ row }">
          <div class="table-actions">
            <el-button v-if="config.edit && editable(row)" type="primary" link @click="edit(row)">
              编辑
            </el-button>
            <el-button v-if="config.shelf" type="primary" link :disabled="busy" @click="shelf(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button
              v-if="route.meta.resource === 'spus'"
              link
              @click="router.push({ path: '/skus', query: { spuId: row.id } })"
            >
              规格
            </el-button>
            <el-button
              v-if="config.remove && editable(row) && (!config.shelf || row.status === 0)"
              type="danger"
              link
              :disabled="busy"
              @click="remove(row)"
            >
              删除
            </el-button>
            <el-button
              v-if="config.publish && row.status === 0"
              type="primary"
              link
              :disabled="busy"
              @click="publish(row, 'publish')"
            >
              发布
            </el-button>
            <el-button
              v-if="config.publish && row.status === 1"
              type="danger"
              link
              :disabled="busy"
              @click="publish(row, 'stop')"
            >
              停止
            </el-button>
            <template v-if="config.user">
              <el-button link type="primary" :disabled="busy" @click="showProfile(row)">
                资料
              </el-button>
              <el-button
                link
                :disabled="busy || String(row.id) === String(session.admin?.userId)"
                @click="userStatus(row)"
              >
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button
                v-if="session.can('rbac:manage')"
                link
                type="primary"
                @click="router.push({ path: '/assignments', query: { userId: row.id } })"
              >
                分配角色
              </el-button>
            </template>
            <el-button
              v-if="config.role"
              type="primary"
              link
              :disabled="busy"
              @click="editPermissions(row)"
            >
              {{ row.roleCode === 'SYSTEM_ADMIN' ? '查看权限' : '权限配置' }}
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="!config.list"
      v-model:current-page="query.current"
      v-model:page-size="query.size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next"
      @size-change="search"
      @current-change="run(load)"
    />
    <p v-if="config.readonly" class="muted section-gap">这里只记录成功的后台写操作。</p>
  </section>
  <el-dialog
    v-model="visible"
    :title="editId ? '编辑记录' : '新增记录'"
    width="760px"
    destroy-on-close
  >
    <ResourceForm ref="formRef" :fields="config.fields" :model="form" :edit-id="editId" />
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="busy" :disabled="formRef?.uploading" @click="save">
        {{ formRef?.uploading ? '图片上传中' : '保存' }}
      </el-button>
    </template>
  </el-dialog>
  <el-dialog v-model="permissionDialog" :title="`${role?.roleName || ''} · 权限`" width="540px">
    <el-checkbox-group v-model="permissionIds" class="permission-grid">
      <el-checkbox
        v-for="p in permissions"
        :key="p.id"
        :value="p.id"
        :disabled="
          role?.roleCode === 'SYSTEM_ADMIN' ||
          (!session.can(p.permissionCode) && !permissionIds.includes(p.id))
        "
      >
        {{ p.permissionName }}
        <span class="muted">{{ p.permissionCode }}</span>
      </el-checkbox>
    </el-checkbox-group>
    <template #footer>
      <el-button @click="permissionDialog = false">关闭</el-button>
      <el-button
        v-if="role?.roleCode !== 'SYSTEM_ADMIN'"
        type="primary"
        :loading="busy"
        @click="savePermissions"
      >
        保存权限
      </el-button>
    </template>
  </el-dialog>
  <el-dialog v-model="profileDialog" title="用户资料" width="520px">
    <el-descriptions v-if="profile" :column="1" border>
      <el-descriptions-item label="昵称">{{ profile.nickname }}</el-descriptions-item>
      <el-descriptions-item label="头像">
        <Picture :src="profile.avatarOssKey" style="width: 80px; height: 80px" />
      </el-descriptions-item>
      <el-descriptions-item label="性别">
        {{ ['保密', '男', '女'][profile.gender] }}
      </el-descriptions-item>
      <el-descriptions-item label="生日">{{ profile.birthday || '—' }}</el-descriptions-item>
      <el-descriptions-item label="简介">{{ profile.bio || '—' }}</el-descriptions-item>
    </el-descriptions>
    <el-empty v-else description="该账号尚无个人资料" />
  </el-dialog>
</template>
