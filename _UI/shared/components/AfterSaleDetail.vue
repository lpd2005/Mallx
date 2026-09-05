<script setup>
import { money, dateTime, saleStates } from '../format.js'
import StatusTag from './StatusTag.vue'
defineProps({ sale: Object })
</script>
<template>
  <template v-if="sale">
    <div class="page-head">
      <span class="mono">{{ sale.afterSaleNo }}</span>
      <StatusTag :value="sale.status" :states="saleStates" />
    </div>
    <el-descriptions :column="1" border>
      <el-descriptions-item label="订单号">{{ sale.orderNo }}</el-descriptions-item>
      <el-descriptions-item label="售后类型">
        {{ sale.type === 1 ? '仅退款' : '退货退款' }}
      </el-descriptions-item>
      <el-descriptions-item label="退款金额">{{ money(sale.refundAmount) }}</el-descriptions-item>
      <el-descriptions-item label="申请原因">{{ sale.reason }}</el-descriptions-item>
      <el-descriptions-item label="申请时间">{{ dateTime(sale.createdAt) }}</el-descriptions-item>
      <el-descriptions-item v-if="sale.auditRemark" label="审核说明">
        {{ sale.auditRemark }}
      </el-descriptions-item>
      <el-descriptions-item v-if="sale.returnTrackingNo" label="退货物流">
        {{ sale.returnCarrier }} / {{ sale.returnTrackingNo }}
      </el-descriptions-item>
      <el-descriptions-item v-if="sale.refundNo" label="退款单号">
        {{ sale.refundNo }}
      </el-descriptions-item>
    </el-descriptions>
    <h3 class="section-gap">处理进度</h3>
    <el-timeline>
      <el-timeline-item v-for="(log, i) in sale.logs" :key="i" :timestamp="dateTime(log.createdAt)">
        {{ saleStates[log.afterStatus] }} · {{ log.remark }}
      </el-timeline-item>
    </el-timeline>
  </template>
</template>
