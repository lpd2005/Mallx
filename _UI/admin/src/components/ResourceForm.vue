<script setup>
import { computed, ref, watch } from 'vue'
import { allPages } from '../services.js'
import ImageUpload from '@mallx/shared/components/ImageUpload.vue'
import SpecEditor from './SpecEditor.vue'
const props = defineProps({ fields: Array, model: Object, editId: [String, Number] }),
  formRef = ref(),
  lookups = ref({}),
  lookupError = ref(''),
  uploading = ref(false)
const active = computed(() => props.fields.filter((f) => !f.when || f.when(props.model)))
const rules = computed(() =>
  Object.fromEntries(
    active.value.map((f) => [
      f.key,
      [
        ...(f.required ? [{ required: true, message: `请填写${f.label}`, trigger: 'blur' }] : []),
        ...(f.pattern ? [{ pattern: f.pattern, message: f.patternMessage, trigger: 'blur' }] : []),
      ],
    ]),
  ),
)
watch(
  () => props.fields,
  async (fields) => {
    lookupError.value = ''
    try {
      const loaded = {}
      await Promise.all(
        fields
          .filter((f) => f.type === 'lookup')
          .map(async (f) => {
            let list = await allPages(f.url)
            if (f.leaf) {
              const parents = new Set(list.map((r) => String(r.parentId)))
              list = list.filter((r) => !parents.has(String(r.id)))
            }
            loaded[f.key] = list.filter((r) => !f.root || String(r.id) !== String(props.editId))
          }),
      )
      lookups.value = loaded
    } catch (e) {
      lookupError.value = e.message
    }
  },
  { immediate: true },
)
defineExpose({ validate: () => formRef.value.validate(), uploading })
</script>
<template>
  <el-alert
    v-if="lookupError"
    :title="`关联选项加载失败：${lookupError}。请关闭后重试。`"
    type="error"
    :closable="false"
    class="error-banner"
  />
  <el-form ref="formRef" :model="model" :rules="rules" label-position="top">
    <div class="form-grid">
      <el-form-item
        v-for="f in active"
        :key="f.key"
        :label="f.label"
        :prop="f.key"
        :class="{ full: f.full }"
      >
        <ImageUpload
          v-if="f.type === 'image'"
          v-model="model[f.key]"
          @uploading="uploading = $event"
        />
        <SpecEditor v-else-if="f.type === 'spec'" v-model="model[f.key]" />
        <el-select v-else-if="f.type === 'select'" v-model="model[f.key]">
          <el-option
            v-for="(label, value) in f.options"
            :key="value"
            :label="label"
            :value="Number(value)"
          />
        </el-select>
        <el-select v-else-if="f.type === 'lookup'" v-model="model[f.key]" filterable>
          <el-option v-if="f.root" :value="0" label="顶级分类" />
          <el-option
            v-for="r in lookups[f.key]"
            :key="r.id"
            :value="r.id"
            :label="`${r[f.name]} · ${r.id}`"
          />
        </el-select>
        <el-input-number
          v-else-if="f.type === 'number'"
          v-model="model[f.key]"
          :min="f.min"
          :max="f.maxNumber ?? 2147483647"
          :precision="f.precision"
          :step="f.precision ? 0.1 : 1"
          controls-position="right"
          style="width: 100%"
        />
        <el-date-picker
          v-else-if="f.type === 'datetime'"
          v-model="model[f.key]"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 100%"
        />
        <el-input
          v-else
          v-model="model[f.key]"
          :type="f.type === 'textarea' ? 'textarea' : 'text'"
          :maxlength="f.max"
          :rows="4"
          show-word-limit
        />
      </el-form-item>
    </div>
  </el-form>
</template>
