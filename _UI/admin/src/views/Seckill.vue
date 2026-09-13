<script setup>
import { inject, onMounted, reactive, ref } from 'vue'
import { dateTime, money } from '@mallx/shared/format.js'
import { confirmAction, useTask } from '@mallx/shared/useTask.js'

const api = inject('api'),
  { busy, error, run } = useTask()
const activities = ref([]),
  status = ref(),
  activityDialog = ref(false),
  activityId = ref(),
  activityForm = reactive({ activityName: '', window: [] }),
  itemDialog = ref(false),
  itemId = ref(),
  itemActivity = ref(),
  itemForm = reactive({ skuId: '', seckillPrice: '', totalStock: 1, limitPerUser: 1 })
const states = {
  0: { text: '草稿', type: 'info' },
  1: { text: '已预热', type: 'success' },
  2: { text: '已关闭', type: 'danger' },
}

async function fetchActivities() {
  activities.value = await api.get('/admin/seckill-activities', { status: status.value })
}
function load() {
  return run(fetchActivities)
}
function openActivity(activity) {
  activityId.value = activity?.activityId
  activityForm.activityName = activity?.activityName || ''
  activityForm.window = activity ? [activity.startAt, activity.endAt] : []
  activityDialog.value = true
}
function saveActivity() {
  run(async () => {
    const name = activityForm.activityName.trim()
    if (!name) throw new Error('请输入活动名称')
    if (!activityForm.window?.[0] || !activityForm.window?.[1]) throw new Error('请选择活动时间')
    const body = {
      activityName: name,
      startAt: activityForm.window[0],
      endAt: activityForm.window[1],
    }
    if (activityId.value) await api.put(`/admin/seckill-activities/${activityId.value}`, body)
    else await api.post('/admin/seckill-activities', body)
    activityDialog.value = false
    await fetchActivities()
  }, '活动已保存')
}
function openItem(activity, item) {
  itemActivity.value = activity
  itemId.value = item?.itemId
  itemForm.skuId = String(item?.skuId || '')
  itemForm.seckillPrice = item?.seckillPrice == null ? '' : String(item.seckillPrice)
  itemForm.totalStock = item?.totalStock || 1
  itemForm.limitPerUser = item?.limitPerUser || 1
  itemDialog.value = true
}
function saveItem() {
  run(async () => {
    const skuId = itemForm.skuId.trim()
    if (!/^[1-9]\d*$/.test(skuId)) throw new Error('请输入有效的SKU ID')
    if (!Number.isFinite(Number(itemForm.seckillPrice)) || Number(itemForm.seckillPrice) <= 0)
      throw new Error('秒杀价格必须大于0')
    const body = {
      skuId,
      seckillPrice: itemForm.seckillPrice,
      totalStock: itemForm.totalStock,
      limitPerUser: itemForm.limitPerUser,
    }
    const base = `/admin/seckill-activities/${itemActivity.value.activityId}/items`
    if (itemId.value) await api.put(`${base}/${itemId.value}`, body)
    else await api.post(base, body)
    itemDialog.value = false
    await fetchActivities()
  }, '秒杀商品已保存')
}
function removeItem(activity, item) {
  run(async () => {
    await confirmAction(`确认从活动中删除SKU ${item.skuId}？`)
    await api.delete(`/admin/seckill-activities/${activity.activityId}/items/${item.itemId}`)
    await fetchActivities()
  }, '秒杀商品已删除')
}
function prepare(activity) {
  run(async () => {
    await confirmAction('预热会锁定配置库存并将活动写入Redis，之后不可修改。确认预热？')
    await api.post(`/admin/seckill-activities/${activity.activityId}/prepare`)
    await fetchActivities()
  }, '活动已完成预热')
}
function close(activity) {
  run(async () => {
    await confirmAction('关闭后立即停止新抢购，并在处理中请求结束后释放剩余预留库存。确认关闭？')
    await api.post(`/admin/seckill-activities/${activity.activityId}/close`)
    await fetchActivities()
  }, '活动已关闭')
}
onMounted(load)
</script>

<template>
  <div class="page-head">
    <div>
      <p class="eyebrow">FLASH SALE</p>
      <h1>秒杀活动</h1>
    </div>
    <el-button type="primary" @click="openActivity()">新建活动</el-button>
  </div>
  <el-alert v-if="error" :title="error" type="error" :closable="false" class="error-banner" />
  <section class="panel">
    <div class="toolbar">
      <el-select v-model="status" placeholder="全部状态" clearable @change="load">
        <el-option
          v-for="(state, value) in states"
          :key="value"
          :label="state.text"
          :value="Number(value)"
        />
      </el-select>
      <el-button :loading="busy" @click="load">刷新</el-button>
    </div>
  </section>
  <section
    v-for="activity in activities"
    :key="activity.activityId"
    class="panel section-gap seckill-admin-card"
  >
    <div class="page-head compact">
      <div>
        <div class="actions">
          <el-tag :type="states[activity.status]?.type">{{ states[activity.status]?.text }}</el-tag>
          <span class="readonly-id">ID {{ activity.activityId }}</span>
        </div>
        <h2>{{ activity.activityName }}</h2>
        <p class="muted">{{ dateTime(activity.startAt) }} — {{ dateTime(activity.endAt) }}</p>
      </div>
      <div class="actions">
        <el-button v-if="activity.status === 0" @click="openActivity(activity)">编辑活动</el-button>
        <el-button v-if="activity.status === 0" type="primary" @click="openItem(activity)">
          添加商品
        </el-button>
        <el-button
          v-if="activity.status === 0"
          type="success"
          :disabled="busy || !activity.items.length"
          @click="prepare(activity)"
        >
          预热开放
        </el-button>
        <el-button
          v-if="activity.status === 1"
          type="danger"
          :disabled="busy"
          @click="close(activity)"
        >
          关闭活动
        </el-button>
      </div>
    </div>
    <el-table :data="activity.items" row-key="itemId">
      <el-table-column prop="itemId" label="商品配置ID" min-width="190" />
      <el-table-column prop="skuId" label="SKU ID" min-width="190" />
      <el-table-column label="秒杀价" width="130">
        <template #default="{ row }">{{ money(row.seckillPrice) }}</template>
      </el-table-column>
      <el-table-column prop="totalStock" label="资格库存" width="110" />
      <el-table-column prop="soldStock" label="已成单" width="100" />
      <el-table-column prop="limitPerUser" label="每人限购" width="110" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="activity.status === 0" label="操作" fixed="right" width="130">
        <template #default="{ row }">
          <el-button link type="primary" @click="openItem(activity, row)">编辑</el-button>
          <el-button link type="danger" :disabled="busy" @click="removeItem(activity, row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!activity.items.length" description="尚未配置秒杀商品" />
  </section>
  <el-empty v-if="!busy && !activities.length" description="暂无秒杀活动" />

  <el-dialog
    v-model="activityDialog"
    :title="activityId ? '编辑秒杀活动' : '新建秒杀活动'"
    width="580px"
    destroy-on-close
  >
    <el-form label-position="top">
      <el-form-item label="活动名称" required>
        <el-input v-model="activityForm.activityName" maxlength="100" show-word-limit />
      </el-form-item>
      <el-form-item label="活动时间" required>
        <el-date-picker
          v-model="activityForm.window"
          type="datetimerange"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DDTHH:mm:ss"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="activityDialog = false">取消</el-button>
      <el-button type="primary" :loading="busy" @click="saveActivity">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="itemDialog"
    :title="itemId ? '编辑秒杀商品' : '添加秒杀商品'"
    width="540px"
    destroy-on-close
  >
    <el-alert
      title="SKU必须已上架；秒杀价不能高于当前销售价，资格库存不能超过真实可用库存。"
      type="info"
      :closable="false"
      class="error-banner"
    />
    <el-form label-position="top">
      <el-form-item label="SKU ID" required>
        <el-input v-model="itemForm.skuId" maxlength="20" />
      </el-form-item>
      <el-form-item label="秒杀价格" required>
        <el-input v-model="itemForm.seckillPrice" inputmode="decimal" maxlength="16" />
      </el-form-item>
      <div class="form-columns">
        <el-form-item label="资格库存" required>
          <el-input-number v-model="itemForm.totalStock" :min="1" :max="2147483647" />
        </el-form-item>
        <el-form-item label="每人限购" required>
          <el-input-number v-model="itemForm.limitPerUser" :min="1" :max="2147483647" />
        </el-form-item>
      </div>
    </el-form>
    <template #footer>
      <el-button @click="itemDialog = false">取消</el-button>
      <el-button type="primary" :loading="busy" @click="saveItem">保存</el-button>
    </template>
  </el-dialog>
</template>
