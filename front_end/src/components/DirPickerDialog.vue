<template>
  <el-dialog v-model="visible" :title="title" width="450px">
    <el-tree
      :data="treeData"
      :props="{ label: 'dirName', children: 'children' }"
      node-key="dirId"
      highlight-current
      accordion
      @node-click="onSelect"
    />
    <div v-if="!treeData.length" style="text-align:center;color:#909399;padding:20px">
      加载中...
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="!selectedDirId" @click="confirm">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  modelValue: Boolean,
  title: { type: String, default: '选择目标目录' },
  /** buildTree(parentId) => Promise<treeNode[]>  */
  buildTree: { type: Function, required: true }
})

const emit = defineEmits(['confirm', 'update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const treeData = ref([])
const selectedDirId = ref(null)

watch(visible, async (v) => {
  if (v) {
    selectedDirId.value = null
    treeData.value = await props.buildTree(null)
  }
})

function onSelect(node) {
  selectedDirId.value = node.dirId
}

function confirm() {
  if (selectedDirId.value) {
    emit('confirm', selectedDirId.value)
    visible.value = false
  }
}
</script>
