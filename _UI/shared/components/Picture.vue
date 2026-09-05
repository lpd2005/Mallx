<script setup>
import { imageSource } from '../format.js'
defineProps({ src: String, alt: { type: String, default: '商品图片' } })
</script>
<template>
  <el-image class="picture" :src="imageSource(src)" :alt="alt" fit="contain" loading="lazy">
    <template #error>
      <div class="image-empty">
        <span>MX</span>
        <small>暂无图片</small>
      </div>
    </template>
  </el-image>
</template>
