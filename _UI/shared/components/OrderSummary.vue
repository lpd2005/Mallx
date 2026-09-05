<script setup>
import Picture from './Picture.vue'
import StatusTag from './StatusTag.vue'
import { money, dateTime, orderStates, specText } from '../format.js'
defineProps({ order: Object })
</script>
<template>
  <template v-if="order">
    <div class="page-head">
      <div>
        <h2>订单详情</h2>
        <span class="mono">{{ order.orderNo }}</span>
      </div>
      <StatusTag :value="order.status" :states="orderStates" />
    </div>
    <el-descriptions :column="1" border>
      <el-descriptions-item label="收货人">
        {{ order.recipientName }} · {{ order.recipientMobile }}
      </el-descriptions-item>
      <el-descriptions-item label="收货地址">{{ order.fullAddress }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ dateTime(order.createdAt) }}</el-descriptions-item>
      <el-descriptions-item v-if="order.expireAt && order.status === 1" label="支付截止">
        {{ dateTime(order.expireAt) }}
      </el-descriptions-item>
      <el-descriptions-item v-if="order.paidAt" label="支付时间">
        {{ dateTime(order.paidAt) }}
      </el-descriptions-item>
      <el-descriptions-item v-if="order.shippedAt" label="发货时间">
        {{ dateTime(order.shippedAt) }}
      </el-descriptions-item>
      <el-descriptions-item v-if="order.completedAt" label="收货时间">
        {{ dateTime(order.completedAt) }}
      </el-descriptions-item>
      <el-descriptions-item v-if="order.cancelReason" label="取消原因">
        {{ order.cancelReason }}
      </el-descriptions-item>
      <el-descriptions-item v-if="order.remark" label="订单备注">
        {{ order.remark }}
      </el-descriptions-item>
    </el-descriptions>
    <div v-for="i in order.items" :key="i.skuId" class="line-item">
      <Picture :src="i.imageOssKey" :alt="i.productName" />
      <div class="info">
        <h3>{{ i.productName }}</h3>
        <p class="muted">{{ i.skuName }} · {{ specText(i.specJson) }}</p>
        <span>{{ money(i.unitPrice) }} × {{ i.quantity }}</span>
      </div>
      <b>{{ money(i.subtotal) }}</b>
    </div>
    <div class="summary-row">
      <span>商品总额</span>
      <span>{{ money(order.productAmount) }}</span>
    </div>
    <div class="summary-row">
      <span>会员优惠 / 优惠券</span>
      <span>
        −{{ money(order.memberDiscountAmount) }} / −{{ money(order.couponDiscountAmount) }}
      </span>
    </div>
    <div class="summary-row">
      <b>应付金额</b>
      <span class="price">{{ money(order.payAmount) }}</span>
    </div>
  </template>
</template>
