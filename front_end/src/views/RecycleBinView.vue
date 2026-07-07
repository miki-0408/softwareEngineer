<template>
  <div class="app-layout">
    <Sidebar active-menu="recycle" />

    <div class="app-main">
      <div class="app-header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item><el-icon><Delete /></el-icon> 回收站</el-breadcrumb-item>
        </el-breadcrumb>
        <span class="text-muted">已删除的文件会保留在此处，可还原或彻底删除</span>
      </div>

      <div class="app-content">
        <!-- 筛选 + 批量操作 -->
        <div class="flex-between mb-16" style="flex-wrap:wrap;gap:8px">
          <div class="flex-center gap-8">
            <el-input v-model="filterText" placeholder="搜索文件..." clearable
              style="width:200px" :prefix-icon="Search" size="default" />
            <el-select v-model="filterType" placeholder="类型" clearable style="width:120px" size="default">
              <el-option label="全部" value="" />
              <el-option v-for="t in fileTypeOptions" :key="t" :label="t" :value="t" />
            </el-select>
            <el-button @click="refreshList">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
          <div v-if="selectedIds.length > 0" class="flex-center gap-8">
            <span class="text-muted">已选 {{ selectedIds.length }} 项</span>
            <el-button type="primary" @click="batchRestore">
              <el-icon><RefreshLeft /></el-icon> 批量还原
            </el-button>
            <el-button type="danger" @click="batchDeletePermanent">
              <el-icon><DeleteFilled /></el-icon> 彻底删除
            </el-button>
          </div>
        </div>

        <el-table
          :data="filteredFiles"
          v-loading="loading"
          empty-text="回收站为空"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="40" />
          <el-table-column label="文件名" min-width="260" prop="fileName" sortable>
            <template #default="{ row }">
              <div class="flex-center gap-8">
                <el-icon :size="20"><Document /></el-icon>
                <span>{{ row.fileName }}</span>
                <el-tag v-if="row.isEncrypted === 1" size="small" type="danger" effect="dark">
                  已加密
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="100" prop="fileType" sortable>
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.fileType || '未知' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="大小" width="120" prop="fileSize" sortable>
            <template #default="{ row }">
              {{ userStore.formatSize(row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column label="删除时间" width="180" prop="uploadTime" sortable>
            <template #default="{ row }">
              {{ formatTime(row.uploadTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" link @click="restoreFile(row)">
                <el-icon><RefreshLeft /></el-icon> 还原
              </el-button>
              <el-button size="small" type="danger" link @click="permanentDeleteFile(row)">
                <el-icon><DeleteFilled /></el-icon> 彻底删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, DeleteFilled, RefreshLeft, Document, Search } from '@element-plus/icons-vue'
import { recycleAPI } from '../api'
import { useUserStore } from '../stores/user'
import Sidebar from '../components/Sidebar.vue'

const userStore = useUserStore()

const recycleFiles = ref([])
const selectedIds = ref([])
const loading = ref(false)

// ==================== 筛选 ====================
const filterText = ref('')
const filterType = ref('')

const fileTypeOptions = computed(() => {
  const types = new Set()
  recycleFiles.value.forEach(f => { if (f.fileType) types.add(f.fileType) })
  return [...types].sort()
})

const filteredFiles = computed(() => {
  let items = recycleFiles.value
  if (filterText.value) {
    const q = filterText.value.toLowerCase()
    items = items.filter(f => f.fileName.toLowerCase().includes(q))
  }
  if (filterType.value) {
    items = items.filter(f => f.fileType === filterType.value)
  }
  return items
})

// ==================== 数据加载 ====================

async function refreshList() {
  loading.value = true
  try {
    const res = await recycleAPI.list()
    recycleFiles.value = res.data.data || []
  } catch { recycleFiles.value = [] }
  finally { loading.value = false }
}

function handleSelectionChange(selection) {
  selectedIds.value = selection.map(f => f.fileId)
}

// ==================== 单项操作 ====================

async function restoreFile(file) {
  try {
    await recycleAPI.restore(file.fileId)
    ElMessage.success('文件已还原')
    await refreshList()
  } catch { /* ignore */ }
}

async function permanentDeleteFile(file) {
  try {
    await ElMessageBox.confirm(
      `文件「${file.fileName}」将被彻底删除，无法恢复！确定继续？`,
      '彻底删除',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error' }
    )
  } catch { return }
  try {
    await recycleAPI.deletePermanent(file.fileId)
    ElMessage.success('已彻底删除')
    await refreshList()
  } catch { /* ignore */ }
}

// ==================== 批量操作 ====================

async function batchRestore() {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定要还原选中的 ${selectedIds.value.length} 个文件吗？`,
      '批量还原', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
    )
  } catch { return }
  let ok = 0
  for (const id of selectedIds.value) {
    try {
      await recycleAPI.restore(id)
      ok++
    } catch { /* skip */ }
  }
  ElMessage.success(`成功还原 ${ok} 个文件`)
  await refreshList()
}

async function batchDeletePermanent() {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `将彻底删除选中的 ${selectedIds.value.length} 个文件，无法恢复！确定继续？`,
      '批量删除',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'error' }
    )
  } catch { return }
  let ok = 0
  for (const id of selectedIds.value) {
    try {
      await recycleAPI.deletePermanent(id)
      ok++
    } catch { /* continue */ }
  }
  ElMessage.success(`已彻底删除 ${ok} 个文件`)
  await refreshList()
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ')
}

onMounted(refreshList)
</script>
