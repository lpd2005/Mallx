<script setup>
import { inject, ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { regionData, codeToText } from 'element-china-area-data'
import { useTask, confirmAction } from '@mallx/shared/useTask.js'
import { safeRedirect } from '@mallx/shared/format.js'
const api = inject('api'),
  route = useRoute(),
  router = useRouter(),
  { busy, error, run } = useTask(),
  addresses = ref([]),
  visible = ref(false),
  editId = ref(),
  formRef = ref(),
  region = ref([])
const initial = () => ({
  recipientName: '',
  recipientMobile: '',
  provinceCode: '',
  provinceName: '',
  cityCode: '',
  cityName: '',
  districtCode: '',
  districtName: '',
  detailAddress: '',
  postalCode: '',
  addressTag: '',
  defaultAddress: false,
})
const form = reactive(initial()),
  rules = {
    recipientName: [{ required: true, message: '请输入收货人姓名' }],
    recipientMobile: [
      { required: true, message: '请输入联系电话' },
      { pattern: /^\+?[0-9 -]{6,20}$/, message: '请输入有效联系电话' },
    ],
    detailAddress: [{ required: true, message: '请输入详细地址' }],
  }
async function load() {
  addresses.value = await api.get('/users/me/addresses')
}
function edit(a) {
  Object.assign(form, initial(), a || {})
  editId.value = a?.id
  region.value = a ? [a.provinceCode, a.cityCode, a.districtCode] : []
  visible.value = true
}
function regionChanged(values) {
  ;['province', 'city', 'district'].forEach((k, i) => {
    form[k + 'Code'] = values?.[i] || ''
    form[k + 'Name'] = codeToText[values?.[i]] || ''
  })
}
async function save() {
  if (!(await formRef.value.validate().catch(() => false))) return
  run(async () => {
    if (region.value.length !== 3) throw new Error('请选择完整的省、市、区县')
    const body = Object.fromEntries(Object.keys(initial()).map((k) => [k, form[k]]))
    if (editId.value) await api.put(`/users/me/addresses/${editId.value}`, body)
    else await api.post('/users/me/addresses', body)
    visible.value = false
    await load()
  }, '地址已保存')
}
function remove(a) {
  run(async () => {
    await confirmAction(`删除 ${a.recipientName} 的这条收货地址？`)
    await api.delete(`/users/me/addresses/${a.id}`)
    await load()
  })
}
function setDefault(a) {
  run(async () => {
    await api.put(`/users/me/addresses/${a.id}/default`)
    await load()
  }, '已设为默认地址')
}
onMounted(() => run(load))
</script>
<template>
  <div class="page-head">
    <h1>收货地址</h1>
    <div class="actions">
      <el-button
        v-if="route.query.redirect"
        @click="router.push(safeRedirect(route.query.redirect))"
      >
        返回结算
      </el-button>
      <el-button type="primary" @click="edit()">新增地址</el-button>
    </div>
  </div>
  <el-alert v-if="error" :title="error" type="error" :closable="false" class="error-banner" />
  <div class="address-grid" v-loading="busy">
    <article v-for="a in addresses" :key="a.id" class="address-card">
      <div class="page-head">
        <h3>{{ a.recipientName }}</h3>
        <el-tag v-if="a.defaultAddress" type="success">默认地址</el-tag>
      </div>
      <p>{{ a.recipientMobile }}</p>
      <p class="muted">
        {{ a.provinceName }}{{ a.cityName }}{{ a.districtName }}{{ a.detailAddress }}
      </p>
      <p v-if="a.addressTag" class="muted">{{ a.addressTag }}</p>
      <div class="actions">
        <el-button link type="primary" @click="edit(a)">编辑</el-button>
        <el-button v-if="!a.defaultAddress" link :disabled="busy" @click="setDefault(a)">
          设为默认
        </el-button>
        <el-button link type="danger" :disabled="busy" @click="remove(a)">删除</el-button>
      </div>
    </article>
  </div>
  <el-empty v-if="!addresses.length && !busy" description="还没有收货地址" />
  <el-dialog
    v-model="visible"
    :title="editId ? '编辑地址' : '新增地址'"
    width="600px"
    destroy-on-close
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <div class="form-grid">
        <el-form-item label="收货人姓名" prop="recipientName">
          <el-input v-model="form.recipientName" maxlength="50" />
        </el-form-item>
        <el-form-item label="联系电话" prop="recipientMobile">
          <el-input v-model="form.recipientMobile" maxlength="20" />
        </el-form-item>
        <el-form-item label="省 / 市 / 区县" class="full" required>
          <el-cascader v-model="region" :options="regionData" filterable @change="regionChanged" />
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress" class="full">
          <el-input v-model="form.detailAddress" type="textarea" maxlength="255" />
        </el-form-item>
        <el-form-item label="邮政编码">
          <el-input v-model="form.postalCode" maxlength="12" />
        </el-form-item>
        <el-form-item label="地址标签">
          <el-input v-model="form.addressTag" maxlength="20" placeholder="家 / 公司" />
        </el-form-item>
      </div>
      <el-checkbox v-model="form.defaultAddress">设为默认地址</el-checkbox>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="busy" @click="save">保存地址</el-button>
    </template>
  </el-dialog>
</template>
