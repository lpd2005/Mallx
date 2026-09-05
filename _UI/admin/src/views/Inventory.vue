<script setup>
import { inject, ref, reactive, onMounted } from 'vue'
import { useTask, confirmAction } from '@mallx/shared/useTask.js'
import { requestNo, dateTime } from '@mallx/shared/format.js'
const api = inject('api'),
  { busy, error, run } = useTask(),
  rows = ref([]),
  total = ref(0),
  current = ref(1),
  skuId = ref(''),
  dialog = ref(false),
  formRef = ref(),
  businessNo = ref(''),
  logs = ref([]),
  logsOpen = ref(false)
const form = reactive({ skuId: '', inboundNo: '', quantity: 1, remark: '' })
const rules = {
  skuId: [
    { required: true, message: '请选择或填写规格ID' },
    { pattern: /^[1-9]\d*$/, message: '规格ID必须为正整数' },
  ],
  inboundNo: [
    { required: true, message: '请输入入库单号' },
    { pattern: /^[A-Za-z0-9_-]{1,48}$/, message: '1～48位字母、数字、下划线或连字符' },
  ],
  quantity: [{ required: true, message: '请输入入库数量' }],
}
async function load() {
  const response = await api.get('/admin/inventory', {
    current: current.value,
    size: 20,
    skuId: skuId.value.trim() || undefined,
  })
  rows.value = response.records
  total.value = Number(response.total)
}
function open(row) {
  Object.assign(form, {
    skuId: row ? String(row.skuId) : '',
    inboundNo: requestNo('IN'),
    quantity: 1,
    remark: '',
  })
  dialog.value = true
}
async function save() {
  if (!(await formRef.value.validate().catch(() => false))) return
  run(async () => {
    await confirmAction(`向规格 ${form.skuId} 入库 ${form.quantity} 件？`)
    await api.post(`/admin/inventory/skus/${form.skuId}/inbound`, {
      inboundNo: form.inboundNo,
      quantity: form.quantity,
      remark: form.remark,
    })
    businessNo.value = `ADM-IN-${form.inboundNo}`
    dialog.value = false
    await load()
  }, '入库成功')
}
function showLogs() {
  run(async () => {
    if (!businessNo.value.trim()) throw new Error('请填写库存业务单号')
    logs.value = await api.get('/admin/inventory/operations', {
      businessNo: businessNo.value.trim(),
    })
    logsOpen.value = true
  })
}
onMounted(() => run(load))
</script>
<template>
  <div class="page-head">
    <h1>库存管理</h1>
    <el-button type="primary" @click="open()">新增入库</el-button>
  </div>
  <el-alert v-if="error" :title="error" type="error" :closable="false" class="error-banner" />
  <section class="panel">
    <div class="toolbar">
      <el-input
        v-model="skuId"
        placeholder="规格ID"
        clearable
        @keyup.enter="
          () => {
            current = 1
            run(load)
          }
        "
      />
      <el-button
        type="primary"
        :loading="busy"
        @click="
          () => {
            current = 1
            run(load)
          }
        "
      >
        查询库存
      </el-button>
    </div>
    <el-table :data="rows" v-loading="busy">
      <el-table-column prop="skuId" label="规格ID" min-width="220" />
      <el-table-column prop="availableStock" label="可用库存" min-width="110" />
      <el-table-column prop="lockedStock" label="锁定库存" min-width="110" />
      <el-table-column prop="soldStock" label="累计扣减量" min-width="130" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.outOfStock ? 'danger' : 'success'">
            {{ row.outOfStock ? '缺货' : '有货' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="open(row)">入库</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="current"
      :total="total"
      :page-size="20"
      layout="total,prev,pager,next"
      @current-change="run(load)"
    />
    <p class="muted section-gap">累计扣减量记录历史库存扣减，退货回补后不会减少。</p>
  </section>
  <section class="panel">
    <h2>库存流水</h2>
    <div class="toolbar">
      <el-input
        v-model="businessNo"
        placeholder="库存业务单号"
        style="width: 380px"
        @keyup.enter="showLogs"
      />
      <el-button :loading="busy" @click="showLogs">查询流水</el-button>
    </div>
    <p class="muted">后台入库业务单号为 ADM-IN- 加入库单号。</p>
  </section>
  <el-dialog v-model="dialog" title="库存入库" width="540px" destroy-on-close>
    <el-alert
      title="重试同一次入库时请保留入库单号和数量，避免重复入库。"
      type="info"
      :closable="false"
      class="error-banner"
    />
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="规格ID" prop="skuId"><el-input v-model="form.skuId" /></el-form-item>
      <el-form-item label="入库单号" prop="inboundNo">
        <el-input v-model="form.inboundNo" maxlength="48" />
      </el-form-item>
      <el-form-item label="入库数量" prop="quantity">
        <el-input-number v-model="form.quantity" :min="1" :max="2147483647" :precision="0" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" maxlength="255" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialog = false">取消</el-button>
      <el-button type="primary" :loading="busy" @click="save">确认入库</el-button>
    </template>
  </el-dialog>
  <el-drawer v-model="logsOpen" title="库存流水" size="min(1000px, 100vw)">
    <el-table :data="logs">
      <el-table-column prop="skuId" label="规格ID" min-width="200" />
      <el-table-column label="操作类型" width="100">
        <template #default="{ row }">
          {{
            { 1: '入库', 2: '锁定', 3: '扣减', 4: '释放', 5: '售后回补' }[row.operationType] ||
            '未知类型'
          }}
        </template>
      </el-table-column>
      <el-table-column prop="quantity" label="数量" width="90" />
      <el-table-column label="可用库存" width="140">
        <template #default="{ row }">{{ row.beforeAvailable }} → {{ row.afterAvailable }}</template>
      </el-table-column>
      <el-table-column label="锁定库存" width="140">
        <template #default="{ row }">{{ row.beforeLocked }} → {{ row.afterLocked }}</template>
      </el-table-column>
      <el-table-column label="时间" min-width="175">
        <template #default="{ row }">{{ dateTime(row.createdAt) }}</template>
      </el-table-column>
    </el-table>
  </el-drawer>
</template>
